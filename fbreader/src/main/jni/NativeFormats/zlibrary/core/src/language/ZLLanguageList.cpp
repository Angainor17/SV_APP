#include <set>

#include <ZLibrary.h>
//#include <ZLResource.h>
#include <ZLFile.h>

#include "ZLLanguageList.h"

std::vector<std::string> ZLLanguageList::ourLanguageCodes;

std::string ZLLanguageList::patternsDirectoryPath() {
    return ZLibrary::ZLibraryDirectory() + ZLibrary::FileNameDelimiter + "languagePatterns";
}

/*std::string ZLLanguageList::languageName(const std::string &code) {
	return ZLResource::resource("language")[ZLResourceKey(code)].value();
}*/

const std::vector<std::string> &ZLLanguageList::languageCodes() {
    if (ourLanguageCodes.empty()) {
        std::set<std::string> codes;
        shared_ptr<ZLDir> dir = ZLFile(patternsDirectoryPath()).directory(false);
        if (!dir.isNull()) {
            std::vector<std::string> fileNames;
            dir->collectFiles(fileNames, false);
            for (std::vector<std::string>::const_iterator it = fileNames.begin();
                 it != fileNames.end(); ++it) {
                const int index = it->find('_');
                if (index != -1) {
                    codes.insert(it->substr(0, index));
                }
            }
        }

        for (std::set<std::string>::const_iterator it = codes.begin(); it != codes.end(); ++it) {
            ourLanguageCodes.push_back(*it);
        }
    }

    return ourLanguageCodes;
}
