/*
 * Copyright (C) 2004-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

#include <ZLFile.h>

#include "PdbPlugin.h"
#include "PalmDocStream.h"
#include "PalmDocLikeStream.h"

#include "../../library/Book.h"

bool PalmDocLikePlugin::providesMetainfo() const {
    return true;
}

shared_ptr<ZLInputStream> PalmDocLikePlugin::createStream(const ZLFile &file) const {
    return new PalmDocContentStream(file);
}

/*
const std::string &PalmDocLikePlugin::tryOpen(const ZLFile &file) const {
	PalmDocStream stream(file);
	stream.open();
	return stream.error();
}
*/
