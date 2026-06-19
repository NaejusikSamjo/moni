package com.moni.user.global.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * 닉네임 자동 생성기
 * 형식: {형용사} {명사}#{6자리 영숫자}
 */
@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "귀여운", "사랑스러운", "똘망한", "씩씩한", "영리한",
            "깜찍한", "포근한", "발랄한", "차분한", "신중한",
            "반짝이는", "활발한", "다정한", "용감한", "현명한",
            "상냥한", "밝은", "따뜻한", "설레는", "진지한",
            "열정적인", "촉망받는", "빛나는", "재빠른", "느긋한",
            "꼼꼼한", "당찬", "늠름한", "유쾌한", "성실한"
    );

    private static final List<String> NOUNS = List.of(
            "주니어", "투자왕", "주식이", "개미투자자", "황소",
            "트레이더", "분석가", "투자자", "펀드왕", "고수",
            "매수왕", "수익러", "포트폴리오", "주주", "배당러",
            "장기투자자", "단타왕", "가치투자자", "퀀트", "ETF왕",
            "코스피왕", "나스닥러", "성장주러", "배당왕", "절약왕",
            "복리왕", "시드머니왕", "수익률왕", "종목발굴러", "미래투자자"
    );

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SUFFIX_LENGTH = 6;

    private final Random random = new Random();

    public String generate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        String suffix = generateSuffix();
        return adjective + " " + noun + "#" + suffix;
    }

    private String generateSuffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}