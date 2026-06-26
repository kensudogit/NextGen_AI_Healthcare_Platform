from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+psycopg://nghealth:nghealth_dev@localhost:5436/nghealth"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    cors_origins: str = "http://localhost:3010"
    dicom_storage_path: str = "storage/dicom"
    hospital_name: str = "NextGen General Hospital"
    hospital_phone: str = "03-1234-5678"

    def cors_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


settings = Settings()
