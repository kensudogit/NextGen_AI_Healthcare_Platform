# PACS Export Sample

これは教育用のPACSエクスポート風サンプルです。

## 内容
- 匿名ダミー患者のDICOM画像（.dcm）
- CT / CR / Secondary Capture のサンプル
- PACSビューア画面サンプル画像
- INDEX.txt に患者・検査・シリーズ構成を記載

## 注意
PACSはファイル形式ではなく、医療画像を保存・配信・閲覧するシステムです。
PACSで扱われる標準画像形式は通常DICOM（.dcm）です。

このサンプルは実患者データではありません。
診断・臨床利用には使用しないでください。

## 推奨ビューア
- Weasis
- RadiAnt DICOM Viewer
- MicroDicom
- Horos / OsiriX系ビューア

## 代表構成
PACS_EXPORT_SAMPLE/
  PATIENT_00000001/
    STUDY_20240520_ACC2024052001/
      SERIES0001_CT_AXIAL/
        IMG000001.dcm
