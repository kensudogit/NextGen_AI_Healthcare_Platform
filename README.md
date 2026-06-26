# NextGen AI Healthcare Platform

AI統合型病院プラットフォーム — Java/Spring Boot バックエンド、React/Next.js フロント、OAuth2、DICOM/PACS、HL7/FHIR、OpenAI、AWS S3。

## 技術スタック

| レイヤ | 技術 |
|--------|------|
| Backend | **Java 21**, **Spring Boot 3.4**, Spring Security **OAuth2**, Spring Data JPA |
| Frontend | **React 19**, **Next.js 15**, TypeScript |
| Database | **PostgreSQL 16** |
| 標準連携 | **FHIR R4** (HAPI FHIR), **HL7 v2** (HAPI HL7), **DICOM** (dcm4che) |
| AI | **OpenAI API** (電話受付・読影要約) |
| Cloud | **AWS S3** (LocalStack でローカルエミュレーション) |
| Auth | **Keycloak** (OAuth2 / OIDC) |

## 機能

| 機能 | API |
|------|-----|
| AI電話受付 | `POST /api/phone/*` |
| 電子カルテ (EMR) | `/api/emr/*` |
| PACS / DICOM | `/api/pacs/*` |
| HL7 v2 | `/api/hl7/*` |
| FHIR R4 | `/fhir/R4/*` |
| 読影AI要約 | `/api/radiology/*` |
| ダッシュボード | `/api/dashboard/*` |

## クイックスタート

```bash
cd NextGen_AI_Healthcare_Platform
cp .env.example .env
docker compose up --build
```

| サービス | URL |
|---------|-----|
| Frontend | http://localhost:3010 |
| Spring Boot API | http://localhost:8010 |
| Swagger UI | http://localhost:8010/swagger-ui.html |
| FHIR Metadata | http://localhost:8010/fhir/R4/metadata |
| Keycloak Admin | http://localhost:8080 (admin / admin) |
| LocalStack S3 | http://localhost:4566 |
| PostgreSQL | localhost:5436 |

## OAuth2 有効化

`.env` で以下を設定:

```env
OAUTH_ENABLED=true
OAUTH2_ISSUER_URI=http://localhost:8080/realms/nghealth
NEXT_PUBLIC_OAUTH_ENABLED=true
```

Keycloak レルム `nghealth` にデモユーザー `staff` / `staff123` をインポート（`keycloak/nghealth-realm.json`）。フロントエンド左サイドバーからログイン。

## ローカル開発

### Backend (Maven)

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## AWS S3 (本番)

```env
STORAGE_USE_S3=true
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=your-bucket
# AWS_ENDPOINT を未設定 → 本番 S3
# IAM ロールまたは AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
```

Docker 開発環境では LocalStack が S3 をエミュレートします。

## レガシー

Python/FastAPI 実装は `_legacy/python-backend/` に保管されています。

## ライセンス

MIT (demo / development use)
"# NextGen_AI_Healthcare_Platform" 
