<h1 align="center">Linguify</h1>

<p align="center">
  Kişiselleştirilmiş İngilizce kelime öğrenme uygulaması — seviyene göre kelimeler, flashcard, AI destekli tekrar ve görsel destekli öğrenme.
</p>

## Demo

<p align="center">
  <img src="https://github.com/user-attachments/assets/3cb1368f-4ba6-488b-87f7-af0e853facf2" alt="Linguify Demo" width="280" />
</p>

---

## Ekran görüntüleri

<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/c6cc3b75-3614-43c1-a12a-3e79a244fac4"alt="Giriş" width="200" /><br/>
      <sub>Giriş</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/c09d5397-fecc-424f-b620-7b230805717b" alt="Ana sayfa" width="200" /><br/>
      <sub>Ana Sayfa</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/3a3ecb0a-46df-4f90-8a76-cf91a0bcfcfc" alt="Öğrenme" width="200" /><br/>
      <sub>Kelime Keşfi</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/ffa2e306-f3c8-45fd-8acc-f0569b40af38" alt="Flashcard" width="200" /><br/>
      <sub>Flashcard</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/e00bf515-776e-49e0-bcab-17da115c2afe" alt="Kelime detay" width="200" /><br/>
      <sub>Kelime Detayı</sub>
    </td>
  </tr>
  <tr>
    <td align="center" colspan="3">
      <img src="https://github.com/user-attachments/assets/b8e28e17-fb56-46da-b6b7-e7ae71457204" alt="Tekrar" width="200" />
      <sub>AI Tekrar</sub>
    </td>
  </tr>
</table>

---

## Hakkında

**Linguify**, CEFR seviyelerine (A1–C2) göre gruplanmış kelime veri setiyle çalışan bir Android İngilizce kelime uygulamasıdır. Kullanıcılar hesap oluşturur, seviye testiyle **Beginner / Intermediate / Advanced** profilini belirler ve kelimeleri **Yeni → Öğrenilecek → Öğreniliyor → Bilinen** durumlarıyla takip eder.

Uygulama; yerel **Room** veritabanı, bulut senkronizasyonu için **Firebase Firestore**, kelime zenginleştirme için **Words API**, görseller için **Pexels** ve tekrar soruları için **Google Gemini** API'lerini bir arada kullanır.

---

## Özellikler

| Alan | Açıklama |
|------|----------|
| Kimlik doğrulama | Firebase Auth ile giriş, kayıt, şifre sıfırlama; Beni hatırla |
| Onboarding | İlk kullanım için kaydırmalı tanıtım ekranları |
| Seviye testi | Kayıt sonrası CEFR tabanlı seviye belirleme |
| Ana sayfa | Kelime sayaçları, ilerleme, haftalık streak, keşif görselleri |
| Kelime öğrenme | Seviyeye uygun kelimeler, Pexels ile görsel |
| Flashcard | Kaydırma ile kart setleri (20'lik setler) |
| Kelime detayı | Tanım, örnek, TTS telaffuz, YouGlish |
| Kelime listeleri | Bilinen / öğrenilecek / öğreniliyor + arama |
| AI tekrar | Gemini ile çoktan seçmeli, bağlam ve tanım soruları |
| Veri | Room + CSV; kullanıcı kelimeleri Firestore'da |

---

## Teknoloji yığını

- Kotlin · MVVM · Hilt · Navigation Component · Coroutines + Flow
- Room · DataStore · Encrypted SharedPreferences
- Firebase Auth · Cloud Firestore
- Retrofit · OkHttp · Glide · Media3 · Lottie · WorkManager

---

## Kullanıcı akışı

Giriş/Kayıt → Onboarding → Seviye testi → Ana sayfa → Öğren / Flashcard / Detay → AI Tekrar → Kelime listeleri

---

<p align="center"><sub>Linguify © 2026</sub></p>
