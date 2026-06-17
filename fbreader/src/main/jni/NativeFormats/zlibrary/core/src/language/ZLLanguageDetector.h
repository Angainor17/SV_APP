#ifndef __ZLLANGUAGEDETECTOR_H__
#define __ZLLANGUAGEDETECTOR_H__

#include <vector>
#include <string>

//#include <shared_ptr.h>

class ZLStatisticsBasedMatcher;

class ZLLanguageDetector {

public:
    struct LanguageInfo {
        LanguageInfo(const std::string &language, const std::string &encoding);

        const std::string Language;
        const std::string Encoding;
    };

public:
    ZLLanguageDetector();

    ~ZLLanguageDetector();

    shared_ptr<LanguageInfo>
    findInfo(const char *buffer, std::size_t length, int matchingCriterion = 0);

    shared_ptr<LanguageInfo>
    findInfoForEncoding(const std::string &encoding, const char *buffer, std::size_t length,
                        int matchingCriterion = 0);

private:
    typedef std::vector<shared_ptr<ZLStatisticsBasedMatcher> > SBVector;
    SBVector myMatchers;
};

#endif /* __ZLLANGUAGEDETECTOR_H__ */
