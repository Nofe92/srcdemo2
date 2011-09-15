/*
 * The MIT License Copyright (C) 2008 Yu Kobayashi http://yukoba.accelart.jp/ Permission is hereby granted, free of charge, to
 * any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package net.decasdev.memoryfs;

import static net.decasdev.dokan.CreationDisposition.CREATE_ALWAYS;
import static net.decasdev.dokan.CreationDisposition.CREATE_NEW;
import static net.decasdev.dokan.CreationDisposition.OPEN_ALWAYS;
import static net.decasdev.dokan.CreationDisposition.OPEN_EXISTING;
import static net.decasdev.dokan.CreationDisposition.TRUNCATE_EXISTING;
import static net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_DIRECTORY;
import static net.decasdev.dokan.FileAttribute.FILE_ATTRIBUTE_NORMAL;
import static net.decasdev.dokan.WinError.ERROR_ALREADY_EXISTS;
import static net.decasdev.dokan.WinError.ERROR_DIRECTORY;
import static net.decasdev.dokan.WinError.ERROR_FILE_EXISTS;
import static net.decasdev.dokan.WinError.ERROR_FILE_NOT_FOUND;
import static net.decasdev.dokan.WinError.ERROR_PATH_NOT_FOUND;
import static net.decasdev.dokan.WinError.ERROR_READ_FAULT;
import static net.decasdev.dokan.WinError.ERROR_WRITE_FAULT;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import net.decasdev.dokan.ByHandleFileInformation;
import net.decasdev.dokan.Dokan;
import net.decasdev.dokan.DokanDiskFreeSpace;
import net.decasdev.dokan.DokanFileInfo;
import net.decasdev.dokan.DokanOperationException;
import net.decasdev.dokan.DokanOperations;
import net.decasdev.dokan.DokanOptions;
import net.decasdev.dokan.DokanVolumeInformation;
import net.decasdev.dokan.FileTimeUtils;
import net.decasdev.dokan.Win32FindData;

public class MemoryFS implements DokanOperations
{
	static final int volumeSerialNumber = 5454;

	static void log(final String msg)
	{
		System.out.println("== app == " + msg);
	}

	public static void main(final String[] args)
	{
		final char driveLetter = (args.length == 0) ? 'S' : args[0].charAt(0);
		new MemoryFS().mount(driveLetter);
		System.exit(0);
	}

	/** fileName -> MemFileInfo */
	final ConcurrentHashMap<String, MemFileInfo> fileInfoMap = new ConcurrentHashMap<String, MemFileInfo>();
	/** Next handle */
	long nextHandleNo = 1;
	final long rootCreateTime = FileTimeUtils.toFileTime(new Date());
	long rootLastWrite = rootCreateTime;
	private final DokanVolumeInformation volumeInfo;

	public MemoryFS()
	{
		showVersions();
		volumeInfo = new DokanVolumeInformation();
		volumeInfo.volumeName = "Dokan Volume";
		volumeInfo.volumeSerialNumber = volumeSerialNumber;
		volumeInfo.fileSystemName = "Mem";
	}

	synchronized long getNextHandle()
	{
		return nextHandleNo++;
	}

	void mount(final char driveLetter)
	{
		final DokanOptions dokanOptions = new DokanOptions();
		dokanOptions.mountPoint = driveLetter + ":\\";
		final int result = Dokan.mount(dokanOptions, this);
		log("[MemoryFS] result = " + result);
	}

	@Override
	public void onCleanup(final String arg0, final DokanFileInfo arg2) throws DokanOperationException
	{
	}

	@Override
	public void onCloseFile(final String arg0, final DokanFileInfo arg1) throws DokanOperationException
	{
	}

	@Override
	public void onCreateDirectory(String fileName, final DokanFileInfo file) throws DokanOperationException
	{
		log("[onCreateDirectory] " + fileName);
		fileName = Utils.trimTailBackSlash(fileName);
		if (fileInfoMap.containsKey(fileName) || fileName.length() == 0) {
			throw new DokanOperationException(ERROR_ALREADY_EXISTS);
		}
		final MemFileInfo fi = new MemFileInfo(fileName, true);
		fileInfoMap.put(fi.fileName, fi);
		updateParentLastWrite(fileName);
	}

	@Override
	public long onCreateFile(final String fileName, final int desiredAccess, final int shareMode,
			final int creationDisposition, final int flagsAndAttributes, final DokanFileInfo arg5)
			throws DokanOperationException
	{
		log("[onCreateFile] " + fileName + ", creationDisposition = " + creationDisposition + ", desiredAccess = "
				+ desiredAccess + ", shareMode = " + shareMode + ", flagsAndAttributes = " + flagsAndAttributes);
		if (fileName.equals("\\")) {
			switch (creationDisposition) {
				case CREATE_NEW:
				case CREATE_ALWAYS:
					throw new DokanOperationException(ERROR_ALREADY_EXISTS);
				case OPEN_ALWAYS:
				case OPEN_EXISTING:
				case TRUNCATE_EXISTING:
					return getNextHandle();
			}
		}
		else if (fileInfoMap.containsKey(fileName)) {
			switch (creationDisposition) {
				case CREATE_NEW:
					throw new DokanOperationException(ERROR_ALREADY_EXISTS);
				case OPEN_ALWAYS:
				case OPEN_EXISTING:
					return getNextHandle();
				case CREATE_ALWAYS:
				case TRUNCATE_EXISTING:
					fileInfoMap.get(fileName).content.clear();
					updateParentLastWrite(fileName);
					return getNextHandle();
			}
		}
		else {
			switch (creationDisposition) {
				case CREATE_NEW:
				case CREATE_ALWAYS:
				case OPEN_ALWAYS:
					final MemFileInfo fi = new MemFileInfo(fileName, false);
					fileInfoMap.put(fi.fileName, fi);
					updateParentLastWrite(fileName);
					return getNextHandle();
				case OPEN_EXISTING:
				case TRUNCATE_EXISTING:
					throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
			}
		}
		throw new DokanOperationException(1);
	}

	@Override
	public void onDeleteDirectory(final String fileName, final DokanFileInfo arg1) throws DokanOperationException
	{
		log("[onDeleteDirectory] " + fileName);
		final MemFileInfo removed = fileInfoMap.remove(fileName);
		if (removed == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		updateParentLastWrite(fileName);
	}

	@Override
	public void onDeleteFile(final String fileName, final DokanFileInfo arg1) throws DokanOperationException
	{
		log("[onDeleteFile] " + fileName);
		final MemFileInfo removed = fileInfoMap.remove(fileName);
		if (removed == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		updateParentLastWrite(fileName);
	}

	@Override
	public Win32FindData[] onFindFiles(final String pathName, final DokanFileInfo arg1) throws DokanOperationException
	{
		log("[onFindFiles] " + pathName);
		final Collection<MemFileInfo> fis = fileInfoMap.values();
		final ArrayList<Win32FindData> files = new ArrayList<Win32FindData>();
		final File pathNameFile = new File(pathName);
		for (final MemFileInfo fi : fis) {
			if (pathNameFile.equals(new File(fi.fileName).getParentFile())) {
				files.add(fi.toWin32FindData());
			}
		}
		log("[onFindFiles] " + files);
		return files.toArray(new Win32FindData[0]);
	}

	@Override
	public Win32FindData[] onFindFilesWithPattern(final String arg0, final String arg1, final DokanFileInfo arg2)
			throws DokanOperationException
	{
		return null;
	}

	@Override
	public void onFlushFileBuffers(final String arg0, final DokanFileInfo arg1) throws DokanOperationException
	{
	}

	@Override
	public DokanDiskFreeSpace onGetDiskFreeSpace(final DokanFileInfo arg0) throws DokanOperationException
	{
		final DokanDiskFreeSpace free = new DokanDiskFreeSpace();
		free.totalNumberOfBytes = 1024 * 1024;
		free.totalNumberOfFreeBytes = 512 * 1024;
		free.freeBytesAvailable = 512 * 1024;
		return free;
	}

	@Override
	public ByHandleFileInformation onGetFileInformation(final String fileName, final DokanFileInfo arg1)
			throws DokanOperationException
	{
		log("[onGetFileInformation] " + fileName);
		if (fileName.equals("\\")) {
			return new ByHandleFileInformation(FILE_ATTRIBUTE_NORMAL | FILE_ATTRIBUTE_DIRECTORY, rootCreateTime,
					rootCreateTime, rootLastWrite, volumeSerialNumber, 0, 1, 1);
		}
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		return fi.toByHandleFileInformation();
	}

	@Override
	public DokanVolumeInformation onGetVolumeInformation(final String arg0, final DokanFileInfo arg1)
			throws DokanOperationException
	{
		return volumeInfo;
	}

	@Override
	public void onLockFile(final String fileName, final long arg1, final long arg2, final DokanFileInfo arg3)
			throws DokanOperationException
	{
		log("[onLockFile] " + fileName);
	}

	@Override
	public void onMoveFile(final String existingFileName, final String newFileName, final boolean replaceExisiting,
			final DokanFileInfo arg3) throws DokanOperationException
	{
		log("==> [onMoveFile] " + existingFileName + " -> " + newFileName + ", replaceExisiting = " + replaceExisiting);
		final MemFileInfo existing = fileInfoMap.get(existingFileName);
		if (existing == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		// TODO Fix this
		if (existing.isDirectory) {
			throw new DokanOperationException(ERROR_DIRECTORY);
		}
		final MemFileInfo newFile = fileInfoMap.get(newFileName);
		if (newFile != null && !replaceExisiting) {
			throw new DokanOperationException(ERROR_FILE_EXISTS);
		}
		fileInfoMap.remove(existingFileName);
		existing.fileName = newFileName;
		fileInfoMap.put(newFileName, existing);
		updateParentLastWrite(existingFileName);
		updateParentLastWrite(newFileName);
		log("<== [onMoveFile]");
	}

	@Override
	public long onOpenDirectory(String pathName, final DokanFileInfo arg1) throws DokanOperationException
	{
		log("[onOpenDirectory] " + pathName);
		if (pathName.equals("\\")) {
			return getNextHandle();
		}
		pathName = Utils.trimTailBackSlash(pathName);
		if (fileInfoMap.containsKey(pathName)) {
			return getNextHandle();
		}
		else {
			throw new DokanOperationException(ERROR_PATH_NOT_FOUND);
		}
	}

	@Override
	public int onReadFile(final String fileName, final ByteBuffer buffer, final long offset, final DokanFileInfo arg3)
			throws DokanOperationException
	{
		log("[onReadFile] " + fileName);
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		if (fi.getFileSize() == 0) {
			return 0;
		}
		try {
			final int copySize = Math.min(buffer.capacity(), fi.getFileSize() - (int) offset);
			if (copySize <= 0) {
				return 0;
			}
			buffer.put(fi.content.toNativeArray((int) offset, copySize));
			return copySize;
		}
		catch (final Exception e) {
			e.printStackTrace();
			throw new DokanOperationException(ERROR_READ_FAULT);
		}
	}

	@Override
	public void onSetEndOfFile(final String fileName, final long length, final DokanFileInfo arg2)
			throws DokanOperationException
	{
		log("[onSetEndOfFile] " + fileName);
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		if (fi.getFileSize() == length) {
			return;
		}
		if (fi.getFileSize() < length) {
			final byte[] tmp = new byte[(int) length - fi.getFileSize()];
			fi.content.add(tmp);
		}
		else {
			fi.content.remove((int) length, fi.getFileSize() - (int) length);
		}
	}

	@Override
	public void onSetFileAttributes(final String fileName, final int fileAttributes, final DokanFileInfo arg2)
			throws DokanOperationException
	{
		log("[onSetFileAttributes] " + fileName);
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		fi.fileAttribute = fileAttributes;
	}

	@Override
	public void onSetFileTime(final String fileName, final long creationTime, final long lastAccessTime,
			final long lastWriteTime, final DokanFileInfo arg4) throws DokanOperationException
	{
		log("[onSetFileTime] " + fileName);
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		fi.creationTime = creationTime;
		fi.lastAccessTime = lastAccessTime;
		fi.lastWriteTime = lastWriteTime;
	}

	@Override
	public void onUnlockFile(final String fileName, final long arg1, final long arg2, final DokanFileInfo arg3)
			throws DokanOperationException
	{
		log("[onUnlockFile] " + fileName);
	}

	@Override
	public void onUnmount(final DokanFileInfo arg0) throws DokanOperationException
	{
		log("[onUnmount]");
	}

	@Override
	public int onWriteFile(final String fileName, final ByteBuffer buffer, final long offset, final DokanFileInfo arg3)
			throws DokanOperationException
	{
		log("[onWriteFile] " + fileName);
		final MemFileInfo fi = fileInfoMap.get(fileName);
		if (fi == null) {
			log("[onWriteFile] file not found");
			throw new DokanOperationException(ERROR_FILE_NOT_FOUND);
		}
		try {
			final int copySize = buffer.capacity();
			final byte[] tmpBuff = new byte[copySize];
			buffer.get(tmpBuff);
			final int overwriteSize = Math.min((int) offset + copySize, fi.content.size()) - (int) offset;
			if (overwriteSize > 0) {
				fi.content.set((int) offset, tmpBuff, 0, overwriteSize);
			}
			final int addSize = copySize - overwriteSize;
			if (addSize > 0) {
				fi.content.add(tmpBuff, overwriteSize, addSize);
			}
			fi.lastWriteTime = FileTimeUtils.toFileTime(new Date());
			return copySize;
		}
		catch (final Exception e) {
			e.printStackTrace();
			throw new DokanOperationException(ERROR_WRITE_FAULT);
		}
	}

	void showVersions()
	{
		final int version = Dokan.getVersion();
		System.out.println("version = " + version);
		final int driverVersion = Dokan.getDriverVersion();
		System.out.println("driverVersion = " + driverVersion);
	}

	void updateParentLastWrite(final String fileName)
	{
		if (fileName.length() <= 1) {
			return;
		}
		final String parent = new File(fileName).getParent();
		log("[updateParentLastWrite] parent = " + parent);
		if (parent == "\\") {
			rootLastWrite = FileTimeUtils.toFileTime(new Date());
		}
		else {
			final MemFileInfo fi = fileInfoMap.get(parent);
			if (fi == null) {
				return;
			}
			fi.lastWriteTime = FileTimeUtils.toFileTime(new Date());
		}
	}
}
