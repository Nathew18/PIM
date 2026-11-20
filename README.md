# Wspólne mieszkanie

Aplikacja na Androida dla współlokatorów: podział wspólnych wydatków („kto komu ile”), wspólna lista zakupów i grafik obowiązków domowych z przypomnieniami.

Projekt na przedmiot **Programowanie interfejsów mobilnych** (PWr, semestr zimowy 2026/27, grupa środa).

## Technologie
- Kotlin + Jetpack Compose (Material 3)
- Android Studio Rabbit 1 | 2026.2.1
- Firebase: Authentication, Cloud Firestore, Cloud Messaging
- Kody QR: ZXing (tworzenie), Google Code Scanner (skanowanie, wymaga Usług Google Play)
- Minimalna wersja: Android 8.0 (API 26)

## Uruchomienie
1. Sklonuj repozytorium: `git clone https://github.com/Nathew18/PIM.git`
2. Otwórz folder w Android Studio (File → Open) i poczekaj na Gradle Sync.
3. Uruchom na emulatorze (Device Manager) albo telefonie (USB / Wi-Fi).

> Plik `app/google-services.json` z Firebase jest w repozytorium. To konfiguracja klienta, nie sekret: dane mają chronić reguły Firestore. Na razie reguły są tymczasowe i wpuszczają każdego zalogowanego, właściwe dojdą w #7, więc nie wpisuj prawdziwych danych.

## Mieszkanie testowe
Do sprawdzenia aplikacji jest przygotowane mieszkanie testowe. Załóż konto, przejdź do zakładki **Mieszkanie** i wpisz kod zaproszenia **`V4LM7R`** w polu „Kod zaproszenia”, a potem kliknij „Dołącz”.

## Struktura kodu
```
app/src/main/java/com/pim/wspolnemieszkanie/
├── LoginActivity.kt       # punkt startowy: logowanie i rejestracja
├── MainActivity.kt        # aplikacja po zalogowaniu (zakładki)
├── AddExpenseActivity.kt  # formularz nowego wydatku, zwraca wynik do MainActivity
├── ui/
│   ├── MainScreen.kt      # Scaffold + dolny pasek zakładek
│   ├── screens/           # ekrany: Logowanie, Wydatki, Zakupy, Obowiązki, Mieszkanie
│   └── theme/             # kolory i typografia (Material 3)
├── data/                  # modele i repozytoria (Firebase)
└── viewmodel/             # ViewModele (MVVM)
```

## Zespół
| Osoba | Zakres |
|---|---|
| Osoba 1 | konfiguracja, Firebase, logowanie, mieszkanie |
| Osoba 2 | wspólne wydatki i saldo |
| Osoba 3 | lista zakupów, obowiązki, powiadomienia |

## Praca z repozytorium
- Każdy task = osobny branch, np. `feature/logowanie`.
- Zmiany na `main` przez Pull Request, przejrzany przez drugą osobę.
- W opisie PR wpisz `Closes #numer`, żeby issue zamknęło się samo.

## Licencja
MIT, zobacz [LICENSE](LICENSE).
