// screens-system.jsx — 디자인 토큰 시트 + 컴포넌트 라이브러리

function Swatch({ c, name, val, dark }) {
  return (
    <div style={{ width: 132 }}>
      <div style={{ height: 64, borderRadius: 14, background: c, border: '1px solid var(--hair)', boxShadow: 'var(--shadow-sm)' }} />
      <div style={{ marginTop: 8, fontSize: 13, fontWeight: 700 }}>{name}</div>
      <div style={{ fontSize: 12, color: 'var(--text-sub)', fontVariantNumeric: 'tabular-nums' }}>{val}</div>
    </div>
  );
}
function Group({ title, children }) {
  return (
    <div>
      <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 14 }}>{title}</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 18 }}>{children}</div>
    </div>
  );
}

function TokenSheet() {
  return (
    <div className="jr" style={{ background: 'var(--bg)', padding: 44, width: 1180 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 6 }}>
        <LogoLockup height={44} />
        <div className="jr-display" style={{ marginLeft: 8 }}>디자인 토큰</div>
      </div>
      <div className="jr-body" style={{ color: 'var(--text-sub)', marginBottom: 34 }}>봄 햇살처럼 따뜻하고 둥근, 그러나 일할 땐 또렷하게 정돈된 시스템.</div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 34 }}>
        <Group title="브랜드 · 포인트 (파스텔 옐로우)">
          <Swatch c="#FFF6DC" name="brand-100" val="#FFF6DC" />
          <Swatch c="#FFE08A" name="brand-300" val="#FFE08A" />
          <Swatch c="#FFD45E" name="brand-500" val="#FFD45E" />
          <Swatch c="#F5B940" name="brand-700" val="#F5B940" />
        </Group>

        <div style={{ display: 'flex', gap: 56, flexWrap: 'wrap' }}>
          <Group title="배경 · 표면 (따뜻한 아이보리)">
            <Swatch c="#FFFCF5" name="bg" val="#FFFCF5" />
            <Swatch c="#FFFFFF" name="surface" val="#FFFFFF" />
            <Swatch c="#FFFBF2" name="surface-soft" val="#FFFBF2" />
          </Group>
          <Group title="텍스트 (브라운그레이)">
            <Swatch c="#43392E" name="text" val="#43392E" />
            <Swatch c="#8C8478" name="text-sub" val="#8C8478" />
            <Swatch c="#B7AFA2" name="text-faint" val="#B7AFA2" />
          </Group>
        </div>

        <div style={{ display: 'flex', gap: 56, flexWrap: 'wrap' }}>
          <Group title="상태색 (전부 부드럽게)">
            <Swatch c="#7FD1AE" name="success" val="#7FD1AE" />
            <Swatch c="#F08C7D" name="warn · 살구코랄" val="#F08C7D" />
            <Swatch c="#FF9E80" name="accent-new" val="#FF9E80" />
          </Group>
          <Group title="누리과정 5영역 색 코딩">
            <Swatch c="#FF9E80" name="신체운동·건강" val="#FF9E80" />
            <Swatch c="#7EC4E8" name="의사소통" val="#7EC4E8" />
            <Swatch c="#FFC94D" name="사회관계" val="#FFC94D" />
            <Swatch c="#C9A8E8" name="예술경험" val="#C9A8E8" />
            <Swatch c="#8FD9A8" name="자연탐구" val="#8FD9A8" />
          </Group>
        </div>

        <hr className="jr-divider" />

        <div style={{ display: 'flex', gap: 56, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <div style={{ flex: '1 1 440px' }}>
            <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 16 }}>타이포그래피 · Pretendard</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              {[['Display', 'jr-display', '28 / 800'], ['Heading 1', 'jr-h1', '24 / 700'], ['Heading 2', 'jr-h2', '20 / 700'], ['Body', 'jr-body', '16 / 500'], ['Caption', 'jr-cap', '13 / 500']].map(([t, c, m]) => (
                <div key={t} style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
                  <span className={c} style={{ flex: 1 }}>{t} · 오늘도 우리 반 함께해요</span>
                  <span style={{ fontSize: 12, color: 'var(--text-faint)', fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap' }}>{m}</span>
                </div>
              ))}
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
                <span className="jr-logo" style={{ flex: 1, fontSize: 28 }}>자람 · 포인트 서체 (로고 전용)</span>
                <span style={{ fontSize: 12, color: 'var(--text-faint)', whiteSpace: 'nowrap' }}>Jua</span>
              </div>
            </div>
          </div>

          <div style={{ flex: '0 0 auto', display: 'flex', gap: 40 }}>
            <div>
              <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 16 }}>라운딩</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {[['카드', 22], ['입력창', 16], ['버튼·칩', 999]].map(([t, r]) => (
                  <div key={t} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{ width: 52, height: 40, borderRadius: r, background: 'var(--brand-100)', border: '1.5px solid var(--brand-300)' }} />
                    <div><div style={{ fontWeight: 700, fontSize: 14 }}>{t}</div><div style={{ fontSize: 12, color: 'var(--text-sub)' }}>{r === 999 ? '999px (알약)' : r + 'px'}</div></div>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 16 }}>그림자 · 간격</div>
              <div style={{ display: 'flex', gap: 14, marginBottom: 18 }}>
                <div style={{ width: 56, height: 56, borderRadius: 16, background: '#fff', boxShadow: 'var(--shadow-sm)' }} />
                <div style={{ width: 56, height: 56, borderRadius: 16, background: '#fff', boxShadow: 'var(--shadow)' }} />
                <div style={{ width: 56, height: 56, borderRadius: 16, background: '#fff', boxShadow: 'var(--shadow-lg)' }} />
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
                {[4, 8, 12, 16, 24, 32].map(s => (
                  <div key={s} style={{ textAlign: 'center' }}>
                    <div style={{ width: s, height: s, background: 'var(--brand-500)', borderRadius: 4, margin: '0 auto' }} />
                    <div style={{ fontSize: 10, color: 'var(--text-sub)', marginTop: 4 }}>{s}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ComponentLib() {
  const Lbl = ({ children }) => <div style={{ fontSize: 13, fontWeight: 800, color: 'var(--text-sub)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 14 }}>{children}</div>;
  return (
    <div className="jr" style={{ background: 'var(--bg)', padding: 44, width: 1180 }}>
      <div className="jr-display" style={{ marginBottom: 30 }}>컴포넌트 라이브러리</div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 40, alignItems: 'start' }}>
        {/* 버튼 */}
        <div>
          <Lbl>버튼 · 알약형 (기본 / 호버 / 비활성)</Lbl>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
            <button className="jr-btn jr-btn--primary">저장하기</button>
            <button className="jr-btn jr-btn--primary" style={{ background: 'var(--brand-700)', transform: 'translateY(-1px)' }}>저장하기</button>
            <button className="jr-btn jr-btn--primary" disabled>저장하기</button>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 12 }}>
            <button className="jr-btn jr-btn--secondary">다시 생성</button>
            <button className="jr-btn jr-btn--ghost">취소</button>
            <button className="jr-btn jr-btn--primary"><Icon name="sparkle" size={20} /> AI 분석</button>
          </div>
        </div>

        {/* 칩 */}
        <div>
          <Lbl>칩 · 태그</Lbl>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 14 }}>
            <NuriChip area="body" /><NuriChip area="comm" /><NuriChip area="social" /><NuriChip area="art" /><NuriChip area="nature" />
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
            <span className="jr-badge-new">NEW</span>
            <span className="jr-badge-count">3</span>
            <span className="jr-toggle">놀이</span>
            <span className="jr-toggle is-on"><Icon name="check" size={14} stroke={2.6} /> 의사소통·상호작용</span>
          </div>
        </div>

        {/* 입력 */}
        <div>
          <Lbl>입력 필드</Lbl>
          <label className="jr-field-label">관찰 내용</label>
          <textarea className="jr-textarea" rows={2} defaultValue="블록놀이 중 친구와 자리를 두고…" />
          <input className="jr-input" style={{ marginTop: 12 }} placeholder="여기에 한 줄 톡톡 적어요" />
        </div>

        {/* 아바타 */}
        <div>
          <Lbl>아바타 · 이니셜</Lbl>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Avatar name="김민준" size="lg" /><Avatar name="박서윤" /><Avatar name="이도윤" size="sm" />
            <Avatar name="한소율" /><Avatar name="최아인" />
          </div>
        </div>

        {/* 메모 카드 */}
        <div>
          <Lbl>메모 카드</Lbl>
          <div className="jr-card">
            <div className="jr-memo">
              <Avatar name="김민준" />
              <div className="body">
                <div className="top"><span className="name">민준이</span><NuriChip area="social" /><span className="time">10:20</span></div>
                <div className="txt">블록놀이 중 친구와 자리를 두고 다퉜지만 스스로 해결함</div>
              </div>
            </div>
          </div>
        </div>

        {/* 피드백 */}
        <div>
          <Lbl>피드백 · 상태</Lbl>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div className="jr-toast"><span className="tick"><Icon name="check" size={14} stroke={3} /></span>민준이 페이지에 저장됐어요</div>
            <div className="jr-banner"><Icon name="sparkle" size={22} style={{ color: 'var(--brand-700)' }} /><span style={{ fontSize: 14, fontWeight: 600 }}>새로 입력된 메모가 있어요. 한 번 더 분석해볼까요?</span></div>
            <div className="jr-banner jr-banner--warn"><Icon name="heart" size={22} style={{ color: 'var(--warn)' }} /><span style={{ fontSize: 14, fontWeight: 700 }}>작성하신 메모는 그대로 저장돼 있어요.</span></div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { TokenSheet, ComponentLib });
