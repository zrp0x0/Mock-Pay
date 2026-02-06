rootProject.name = "mock-pay"

// 두 개의 지사(모듈)
include("db-core")  // 데이터베이스와 관련된 핵심 로직 (육수 공장)
include("api")      // 사용자 요청을 받는 서버 (레스토랑 매장)