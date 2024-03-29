/*
 * JDokan : Java library for Dokan Copyright (C) 2008 Yu Kobayashi http://yukoba.accelart.jp/ http://decas-dev.net/en This
 * program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.decasdev.dokan;

public class DokanDiskFreeSpace {
	public long freeBytesAvailable;
	public long totalNumberOfBytes;
	public long totalNumberOfFreeBytes;

	public DokanDiskFreeSpace() {
	}

	public DokanDiskFreeSpace(final long freeBytes, final long totalFree, final long totalBytes) {
		freeBytesAvailable = freeBytes;
		totalNumberOfFreeBytes = totalFree;
		totalNumberOfBytes = totalBytes;
	}

	@Override
	public String toString() {
		return "DokanDiskFreeSpace(" + "freeBytesAvailable=" + freeBytesAvailable + "," + "totalNumberOfBytes="
			+ totalNumberOfBytes + "," + "totalNumberOfFreeBytes=" + totalNumberOfFreeBytes + ")";
	}
}
