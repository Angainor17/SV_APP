#ifndef __ZLLANGUAGEMATCHER_H__
#define __ZLLANGUAGEMATCHER_H__

#include "ZLLanguageDetector.h"

#include "ZLStatistics.h"

class ZLLanguageMatcher {

public:
    ZLLanguageMatcher(shared_ptr<ZLLanguageDetector::LanguageInfo> info);

    virtual ~ZLLanguageMatcher();

    shared_ptr<ZLLanguageDetector::LanguageInfo> info() const;

private:
    shared_ptr<ZLLanguageDetector::LanguageInfo> myInfo;
};

class ZLStatisticsBasedMatcher : public ZLLanguageMatcher {

public:
    ZLStatisticsBasedMatcher(const std::string &fileName,
                             shared_ptr<ZLLanguageDetector::LanguageInfo> info);

    ~ZLStatisticsBasedMatcher(); // надо ли его объявлять, если он ничего не делает??

    int charSequenceLength() const;

    int criterion(const ZLStatistics &otherStatistics) const;

private:
    shared_ptr<ZLArrayBasedStatistics> myStatisticsPtr;
};

#endif /* __ZLLANGUAGEMATCHER_H__ */
