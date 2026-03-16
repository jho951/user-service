name: 'Pull Request'
description: '기능 개발, 수정, 버그 수정 등을 위한 Pull Request 템플릿입니다.'

body:
  - type: markdown
    attributes:
      value: |
        ## 📌 변경 사항
        아래에 이번 PR에서 변경한 내용을 간단히 작성해주세요.
        예시:
        - Redis 설정 추가
        - 로그인 토큰 Redis 저장 기능 구현

  - type: checkboxes
    attributes:
      label: 🛠 작업 유형
      description: 해당하는 항목을 선택해주세요.
      options:
        - label: 코드 수정
        - label: 문서 수정
        - label: 테스트 추가/수정
        - label: 의존성 추가/변경
        - label: 설정 파일 수정
        - label: 기타

  - type: input
    attributes:
      label: 🔗 관련 이슈
      description: 관련된 이슈 번호를 입력해주세요.
      placeholder: 예) #3, #8

  - type: checkboxes
    attributes:
      label: ✅ 확인 사항
      description: PR을 제출하기 전에 아래 항목들을 확인해주세요.
      options:
        - label: 코드 스타일을 지켰습니다.
        - label: 테스트가 통과되었고 수정이 반영되었습니다.
        - label: 관련 문서가 최신화되었습니다.
        - label: 의존성 변경이 필요한 경우 검토를 완료했습니다.
        - label: CI/CD 파이프라인이 정상 작동하는지 확인했습니다.

  - type: textarea
    attributes:
      label: 💬 추가 설명
      description: 특별히 리뷰어에게 알리고 싶은 내용이 있다면 작성해주세요.
      placeholder: 예) 로그인 토큰 관리를 위해 Redis 도입 / 테스트 코드 부족
