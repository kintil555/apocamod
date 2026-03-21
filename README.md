# ☠ Apocalypse Mod — Kiamat Chaos ☠

Mod Minecraft Fabric 1.21.1 yang membawa **kiamat chaos** ke dunia kamu.

---

## 📖 Cara Kerja

### Entitas Doppelganger
Setiap 30 detik, **bayangan dirimu sendiri** (Doppelganger) akan muncul secara random di sekitar setiap player dalam radius 10–30 blok.

- Doppelganger akan **aktif mengejarmu** dan menyerangmu
- Mereka diberi nama **"☠ [NamaMu]'s Shadow ☠"**
- Maksimal **2 Doppelganger per player** pada waktu bersamaan

### Cara Memicu Kiamat
**Bunuh Doppelganger milikmu sendiri** → Kiamat langsung dimulai!

> ⚠️ Hati-hati: jika Doppelganger dibunuh oleh player lain, kiamat tidak terpicu.

---

## 💀 Fase-Fase Kiamat Chaos

| Level | Fase | Efek |
|-------|------|------|
| 0–25% | Awal | Badai petir abadi, malam permanen, petir acak |
| 25–50% | Eskalasi | Ledakan acak, semua mob hostile diperkuat (Speed+Strength III) |
| 50–75% | Chaos | Bola api dari langit, geyser lava, gravitasi kacau (Levitation), Doppelganger makin sering muncul |
| 75–100% | Kehancuran | Ledakan besar setiap 10 detik, dunia merobek nyawa player (2 damage tiap 5 detik) |
| 100% | TOTAL CHAOS | Semua efek di atas + ledakan & petir tiap 2 detik, pesan kiamat total |

Kiamat **makin parah seiring waktu** (mencapai 100% dalam ~5 menit).

---

## 🔧 Commands (Operator / Level 2)

| Command | Fungsi |
|---------|--------|
| `/apocalypse trigger` | Memicu kiamat secara manual (untuk testing, tanpa perlu bunuh Doppelganger) |
| `/apocalypse reset` | Reset kiamat dan kembalikan dunia normal |
| `/apocalypse status` | Cek apakah kiamat sedang berjalan dan level-nya |
| `/apocalypse spawn_doppelganger` | Spawn Doppelgang milikmu tepat di depanmu (untuk testing) |

---

## 🔨 Build & Instalasi

### Prasyarat
- Java 21
- Fabric Loader ≥ 0.16.0
- Fabric API 0.102.0+1.21.1

### Build Manual
```bash
./gradlew build
```
Hasil `.jar` ada di `build/libs/apocalypsemod-1.0.0.jar`

### Build via GitHub Actions
Push ke branch `main` atau `master` → otomatis build dan upload artifact.

### Instalasi
1. Install [Fabric Loader](https://fabricmc.net/use/installer/) untuk Minecraft 1.21.1
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Taruh `apocalypsemod-1.0.0.jar` dan `fabric-api-*.jar` di folder `mods/`
4. Jalankan Minecraft!

---

## ⚙️ Kompatibilitas
- Minecraft: **1.21.1**
- Fabric Loader: **≥ 0.16.0**
- Fabric API: **0.102.0+1.21.1**
- Java: **21**

---

## 📜 Lisensi
MIT License
