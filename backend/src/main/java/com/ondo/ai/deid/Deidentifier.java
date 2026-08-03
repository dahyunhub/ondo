package com.ondo.ai.deid;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 비식별화 정책 컴포넌트(스테이트리스). 아동 실명을 요청 스코프 센티넬 [[CHILD_n]]로 치환한다.
 * 분석 경로에서만 사용하며 메모 저장 경로에는 개입하지 않는다(SM-1 입력속도 가드).
 * 인프라(OpenAiClient)를 직접 참조하지 않는다 — 의존성 방향 고정. 정본: docs/specs/ai-integration-spec.md §5.
 */
@Component
public class Deidentifier {

    /**
     * 반 전체 명단으로 요청 스코프 매핑을 만든다(MVP: 대상 아이뿐 아니라 본문에 등장할 수 있는 타 아동명도 보강 치환).
     * 토큰 번호는 명단 제공 순서대로 1..n(결정적). 빈/공백/중복 실명은 무시(중복 실명은 같은 토큰 재사용).
     */
    public RestorationContext newContext(Collection<String> rosterNames) {
        Map<String, String> nameToTokenByOrder = new LinkedHashMap<>();
        Map<String, String> tokenToName = new LinkedHashMap<>();
        int n = 1;
        for (String name : rosterNames) {
            if (name == null || name.isBlank() || nameToTokenByOrder.containsKey(name)) {
                continue;
            }
            String token = RestorationContext.token(n);
            nameToTokenByOrder.put(name, token);
            tokenToName.put(token, name);
            n++;
        }
        addSurnameStrippedAliases(nameToTokenByOrder);
        // 치환은 부분문자열 오치환 방지를 위해 '긴 이름부터'(예: "김민준"을 "김민"보다 먼저).
        Map<String, String> nameToTokenLongestFirst = new LinkedHashMap<>();
        nameToTokenByOrder.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
                .forEach(e -> nameToTokenLongestFirst.put(e.getKey(), e.getValue()));
        return new RestorationContext(nameToTokenLongestFirst, tokenToName);
    }

    /**
     * 성을 뗀 이름(호칭)을 같은 토큰으로 보강한다 — 교사·시드는 "고은서"가 아니라 "은서"로 적는 경우가 많다.
     * 이 별칭이 없으면 축약형 실명이 마스킹 없이 외부 AI로 나가고(NFR-1 유출), 대상 아이가 토큰화되지 않아
     * 오지칭(엉뚱한 아이 이름 삽입)의 빌미가 된다. 원본 map(풀네임)에 별칭을 덧붙인다(토큰은 풀네임과 공유).
     *
     * 오치환을 줄이는 가드:
     *  - 2글자 이상만(1글자 별칭은 일반어와 충돌 위험이 큼). 단일 성 1글자 제거 기준(복성은 보강 대상 아님 — 무해).
     *  - 반 안에서 유일한 축약형만(동명이인의 축약형은 어느 실명인지 모호 → 제외).
     *  - 다른 아이의 풀네임과 겹치면 제외(그 풀네임 매핑을 침해하지 않도록).
     * 한계: "하나·보라"처럼 이름이 일반어와 같은 2글자면 본문의 그 단어까지 치환될 수 있다(축약형 비식별화의 본질적 한계).
     */
    private void addSurnameStrippedAliases(Map<String, String> nameToToken) {
        Map<String, Long> shortNameCount = new HashMap<>();
        Map<String, String> shortNameToToken = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : nameToToken.entrySet()) {
            String full = entry.getKey();
            if (full.length() < 3) {
                continue; // 성을 떼도 2글자가 남는 이름만
            }
            String shortName = full.substring(1); // 성 1글자 제거
            shortNameCount.merge(shortName, 1L, Long::sum);
            shortNameToToken.putIfAbsent(shortName, entry.getValue());
        }
        for (Map.Entry<String, String> entry : shortNameToToken.entrySet()) {
            String shortName = entry.getKey();
            if (shortNameCount.get(shortName) > 1 || nameToToken.containsKey(shortName)) {
                continue; // 모호(동명이인) 또는 다른 아이 풀네임과 충돌 → 별칭 보류
            }
            nameToToken.put(shortName, entry.getValue());
        }
    }

    /** 텍스트의 아동 실명을 센티넬 토큰으로 치환한다(긴 이름 우선). 분석 입력의 모든 텍스트 필드에 적용 가능. */
    public String deidentify(String text, RestorationContext context) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (Map.Entry<String, String> entry : context.nameToToken().entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
