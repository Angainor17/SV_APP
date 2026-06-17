#ifndef __ZLLANGUAGELIST_H__
#define __ZLLANGUAGELIST_H__

#include <vector>

#include <ZLDir.h>

class ZLLanguageList {

public:
    static std::string patternsDirectoryPath();

    static const std::vector<std::string> &languageCodes();
    //static std::string languageName(const std::string &code);

private:
    static std::vector<std::string> ourLanguageCodes;

private:
    ZLLanguageList();
};

#endif /* __ZLLANGUAGELIST_H__ */
