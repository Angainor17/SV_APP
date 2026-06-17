/*
 * Copyright (C) 2010-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.network.opds

import org.geometerplus.fbreader.network.atom.ATOMConstants

interface OPDSConstants : ATOMConstants {
    companion object {
        // Feed level
        const val REL_BOOKSHELF = "http://data.fbreader.org/rel/bookshelf"
        const val REL_RECOMMENDATIONS = "http://data.fbreader.org/rel/recommendations"
        const val REL_TOPUP = "http://data.fbreader.org/rel/topup"
        //const val REL_SUBSCRIPTIONS = "http://opds-spec.org/subscriptions"

        // Entry level / catalog types
        const val REL_SUBSECTION = "subsection"

        // Entry level / acquisition links
        const val REL_ACQUISITION_PREFIX = "http://opds-spec.org/acquisition"
        const val REL_FBREADER_ACQUISITION_PREFIX = "http://data.fbreader.org/acquisition"
        const val REL_ACQUISITION = "http://opds-spec.org/acquisition"
        const val REL_ACQUISITION_OPEN = "http://opds-spec.org/acquisition/open-access"
        const val REL_ACQUISITION_SAMPLE = "http://opds-spec.org/acquisition/sample"
        const val REL_ACQUISITION_BUY = "http://opds-spec.org/acquisition/buy"
        //const val REL_ACQUISITION_BORROW = "http://opds-spec.org/acquisition/borrow"
        //const val REL_ACQUISITION_SUBSCRIBE = "http://opds-spec.org/acquisition/subscribe"
        const val REL_ACQUISITION_CONDITIONAL = "http://data.fbreader.org/acquisition/conditional"
        const val REL_ACQUISITION_SAMPLE_OR_FULL = "http://data.fbreader.org/acquisition/sampleOrFull"

        // Entry level / other
        const val REL_IMAGE_PREFIX = "http://opds-spec.org/image"
        //const val REL_IMAGE = "http://opds-spec.org/image"
        const val REL_IMAGE_THUMBNAIL = "http://opds-spec.org/image/thumbnail"
        // FIXME: This relations have been removed from OPDS-1.0 standard. Use RelationAlias instead???
        const val REL_COVER = "http://opds-spec.org/cover"
        const val REL_THUMBNAIL = "http://opds-spec.org/thumbnail"
        const val REL_CONTENTS = "contents" // Book TOC
        const val REL_REPLIES = "replies"
        const val REL_RELATED = "related"
        const val REL_ALTERNATE = "alternate"

        // Entry level / OPDS Link Relations
        const val REL_LINK_SIGN_IN = "http://data.fbreader.org/catalog/sign-in"
        const val REL_LINK_SIGN_OUT = "http://data.fbreader.org/catalog/sign-out"
        const val REL_LINK_SIGN_UP = "http://data.fbreader.org/catalog/sign-up"
        const val REL_LINK_TOPUP = "http://data.fbreader.org/catalog/refill-account"
        const val REL_LINK_RECOVER_PASSWORD = "http://data.fbreader.org/catalog/recover-password"
    }
}
