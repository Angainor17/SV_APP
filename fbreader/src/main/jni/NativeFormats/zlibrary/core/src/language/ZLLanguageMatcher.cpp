#include <ZLFile.h>
#include <ZLInputStream.h>

#include "ZLLanguageMatcher.h"
#include "ZLStatistics.h"
#include "ZLStatisticsXMLReader.h"

ZLLanguageMatcher::ZLLanguageMatcher(shared_ptr<ZLLanguageDetector::LanguageInfo> info) : myInfo(
        info) {
}

ZLLanguageMatcher::~ZLLanguageMatcher() {
}

shared_ptr<ZLLanguageDetector::LanguageInfo> ZLLanguageMatcher::info() const {
    return myInfo;
}

ZLStatisticsBasedMatcher::ZLStatisticsBasedMatcher(const std::string &fileName,
                                                   shared_ptr<ZLLanguageDetector::LanguageInfo> info)
        : ZLLanguageMatcher(info) {
    myStatisticsPtr = ZLStatisticsXMLReader().readStatistics(fileName);
    //if (myStatisticsPtr == 0) {
    //std::cerr << "pattern reading failed\n";
    //}
}

ZLStatisticsBasedMatcher::~ZLStatisticsBasedMatcher() {
}

int ZLStatisticsBasedMatcher::charSequenceLength() const {
    return myStatisticsPtr->getCharSequenceSize();
}

int ZLStatisticsBasedMatcher::criterion(const ZLStatistics &otherStatistics) const {
    return ZLStatistics::correlation(otherStatistics, *myStatisticsPtr);
}
