// data.jsx — 자람 샘플 데이터 (햇살반)
window.JR_DATA = {
  teacher: '김민서',
  cls: '햇살반',
  date: '6월 14일 토요일',

  // 가나다순 명단 (생년월일 / 성별)
  children: [
    { name: '강하준', memos: 7,  birth: '2020.04.12', sex: '남' },
    { name: '김민준', memos: 12, birth: '2020.11.03', sex: '남' },
    { name: '박서윤', memos: 9,  birth: '2021.02.27', sex: '여' },
    { name: '서지호', memos: 5,  birth: '2020.07.19', sex: '남' },
    { name: '이도윤', memos: 8,  birth: '2021.01.08', sex: '남' },
    { name: '정시우', memos: 6,  birth: '2020.09.30', sex: '여' },
    { name: '최아인', memos: 11, birth: '2020.05.21', sex: '여' },
    { name: '한소율', memos: 10, birth: '2021.03.14', sex: '여' },
  ],

  // 담당 반 (같은 반 이름·다른 학년도는 별개의 반)
  classes: [
    { name: '달님반', count: 24, year: '2026학년도', tag: '올해' },
    { name: '햇살반', count: 22, year: '2025학년도', tag: '작년' },
  ],

  // 오늘 쌓인 메모 (홈 피드 / 일지 분석 입력)
  todayMemos: [
    { name: '김민준', txt: '블록놀이 중 친구와 자리를 두고 다퉜지만 스스로 “같이 쓰자”며 해결함', area: 'social', time: '10:20' },
    { name: '박서윤', txt: '바깥놀이에서 달리다 넘어진 친구를 먼저 일으켜 주고 괜찮은지 물어봄', area: 'body', time: '11:05' },
    { name: '이도윤', txt: '자유놀이 때 물감으로 봄꽃을 그리며 노랑·빨강 색 섞기를 여러 번 시도함', area: 'art', time: '13:40' },
    { name: '한소율', txt: '동화를 듣고 “나비는 왜 꽃을 좋아해요?”라고 질문, 곤충에 관심을 보임', area: 'nature', time: '14:15' },
  ],
  // 재분석 화면에서 추가된 새 메모 (NEW)
  newMemos: [
    { name: '김민준', txt: '점심시간에 처음 온 친구에게 먼저 다가가 자리를 안내해 줌', area: 'comm', time: '15:02', isNew: true },
  ],

  // 오늘 전체 분석 → 하루 일지 초안 (누리과정 영역별)
  journalDraft: {
    title: '6월 14일 일지 · 햇살반',
    blocks: [
      { area: 'social', txt: '블록 영역에서 또래 간 자리 다툼이 있었으나, 유아 스스로 “같이 쓰자”는 제안을 통해 갈등을 해결하는 모습이 관찰됨. 양보와 협력의 태도가 자라고 있음.' },
      { area: 'body', txt: '바깥놀이 중 넘어진 친구를 살피고 일으켜 주는 등 신체활동 속에서 배려 행동이 나타남. 대근육 활동에 적극적으로 참여함.' },
      { area: 'art', txt: '물감을 활용한 봄꽃 표현 활동에서 색 혼합을 반복 시도하며 색의 변화에 흥미를 보임. 자기 생각을 색으로 표현하는 즐거움을 경험함.' },
      { area: 'nature', txt: '동화 감상 후 곤충과 식물의 관계에 대한 호기심 어린 질문이 이어짐. 주변 자연현상에 대한 탐구 의지가 관찰됨.' },
    ],
  },

  // 한 아이 분석 (김민준) → 개인 관찰 평가
  childAnalysis: {
    name: '김민준',
    summary: '이번 달 민준이는 또래관계에서 긍정적인 변화가 여러 번 관찰되었어요. 갈등 상황에서 감정을 조절하고 먼저 해결책을 제안하는 모습이 늘었고, 새로운 친구에게 다가가는 사회적 자신감도 자라고 있습니다.',
    areas: [
      { area: 'social', txt: '자리 다툼을 스스로 “같이 쓰자”로 해결, 새 친구에게 먼저 다가감 — 협력·배려가 꾸준히 성장.' },
      { area: 'comm', txt: '자신의 생각과 제안을 또래에게 분명한 말로 전달하는 표현력이 향상됨.' },
    ],
  },

  // 김민준 타임라인 (시간순, 최신이 위)
  timeline: [
    { date: '6월 15일', time: '13:20', area: 'uncat', txt: '점심 후 그림책을 골라 혼자 조용히 들여다보며 페이지를 넘김.' },
    { date: '6월 14일', time: '15:02', area: 'comm', txt: '점심시간에 처음 온 친구에게 먼저 다가가 자리를 안내해 줌.' },
    { date: '6월 14일', time: '10:20', area: 'social', txt: '블록놀이 중 자리 다툼을 스스로 “같이 쓰자”며 해결함.' },
    { date: '6월 12일', time: '11:30', area: 'body', txt: '바깥놀이에서 두발 모아 멀리뛰기에 반복해서 도전, 즐거워함.' },
    { date: '6월 10일', time: '13:15', area: 'social', txt: '역할놀이에서 친구에게 “네가 먼저 해”라며 순서를 양보함.' },
    { date: '6월 7일',  time: '09:50', area: 'nature', txt: '텃밭의 상추 잎을 만져보며 “보들보들해요” 하고 촉감을 표현함.' },
    { date: '6월 5일',  time: '14:40', area: 'art', txt: '점토로 자기 가족을 만들며 인물마다 색을 다르게 골라 표현함.' },
  ],

  // 명단에서 제외(soft delete)된 아이 예시
  hiddenChild: { name: '윤서연', birth: '2020.08.11' },

  // 개인평가 이력 (시간순 누적 · 덮어쓰지 않음)
  childEvals: [
    { title: '6월 관찰평가', period: '6월 1일 ~ 오늘', time: '6.10 20:02', kind: '수동', areas: ['social', 'comm'] },
    { title: '5월 관찰평가', period: '5월 1일 ~ 5월 31일', time: '5.31 자동 생성', kind: '자동', areas: ['social', 'body', 'nature'] },
    { title: '4월 관찰평가', period: '4월 1일 ~ 4월 30일', time: '4.30 자동 생성', kind: '자동', areas: ['art', 'comm'] },
  ],

  // 마이 · 교사 정보
  teacherEmail: 'minseo@jaram.kr',

  // 일지·분석 기록 (최신순)
  journalHistory: [
    { date: '6월 14일', type: '하루 일지', time: '오늘 14:30', areas: ['social', 'body', 'art', 'nature'] },
    { date: '6월 13일', type: '하루 일지', time: '어제 19:10', areas: ['comm', 'social', 'nature'] },
    { date: '6월 10일', type: '김민준 관찰평가', time: '6.10 20:02', areas: ['social', 'comm'], child: '김민준' },
    { date: '6월 12일', type: '하루 일지', time: '6.12 18:45', areas: ['body', 'art'] },
  ],
};
