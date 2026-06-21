<h1 align="center">Linguify</h1>
<p align="center">
  A personalized English vocabulary learning app — words tailored to your level, flashcards, AI-powered review, and visually-assisted learning.
</p>

## Demo
<p align="center">
  <img src="https://github.com/user-attachments/assets/3cb1368f-4ba6-488b-87f7-af0e853facf2" alt="Linguify Demo" width="280" />
</p>

---

## Screenshots
<table>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/c6cc3b75-3614-43c1-a12a-3e79a244fac4" alt="Login" width="200" /><br/>
      <sub>Login</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/c09d5397-fecc-424f-b620-7b230805717b" alt="Home" width="200" /><br/>
      <sub>Home</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/3a3ecb0a-46df-4f90-8a76-cf91a0bcfcfc" alt="Learning" width="200" /><br/>
      <sub>Word Discovery</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/ffa2e306-f3c8-45fd-8acc-f0569b40af38" alt="Flashcard" width="200" /><br/>
      <sub>Flashcard</sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/e00bf515-776e-49e0-bcab-17da115c2afe" alt="Word detail" width="200" /><br/>
      <sub>Word Detail</sub>
    </td>
  </tr>
  <tr>
    <td align="center" colspan="3">
      <img src="https://github.com/user-attachments/assets/b8e28e17-fb56-46da-b6b7-e7ae71457204" alt="Review" width="200" /><br/>
      <sub>AI Review</sub>
    </td>
  </tr>
</table>

---

## About
**Linguify** is an Android English vocabulary app built on a word dataset grouped by CEFR levels (A1–C2). Users create an account, determine their **Beginner / Intermediate / Advanced** profile through a level test, and track words through **New → To Learn → Learning → Known** states.

The app combines a local **Room** database, **Firebase Firestore** for cloud sync, the **Words API** for word enrichment, **Pexels** for imagery, and **Google Gemini** for review questions.

---

## Features
| Area | Description |
|------|--------------|
| Authentication | Sign in, sign up, and password reset via Firebase Auth; remember me |
| Onboarding | Swipeable intro screens for first-time use |
| Level test | CEFR-based level assessment after sign-up |
| Home | Word counters, progress, weekly streak, discovery visuals |
| Word learning | Level-appropriate words with images from Pexels |
| Flashcard | Swipeable card sets (sets of 20) |
| Word detail | Definition, example sentence, TTS pronunciation, YouGlish |
| Word lists | Known / to learn / learning + search |
| AI review | Multiple-choice, context, and definition questions via Gemini |
| Data | Room + CSV; user words stored in Firestore |

---

## Tech Stack
- Kotlin · MVVM · Hilt · Navigation Component · Coroutines + Flow
- Room · DataStore · Encrypted SharedPreferences
- Firebase Auth · Cloud Firestore
- Retrofit · OkHttp · Glide · Media3 · Lottie · WorkManager

---

## User Flow
Login/Sign Up → Onboarding → Level Test → Home → Learn / Flashcard / Detail → AI Review → Word Lists
