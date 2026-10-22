package com.tempora.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// Satu baris = satu aplikasi yang dibatasi. Aplikasi yang bebas dibuka tidak punya baris.
// paket adalah nama unik aplikasi di Android, misalnya com.instagram.android.
@Entity(tableName = "aplikasi_dibatasi")
data class AplikasiDibatasi(
    @PrimaryKey val paket: String,
    val batasMenit: Int,
)

// Dao = daftar perintah database untuk satu tabel. Fungsi yang mengembalikan Flow akan
// mengirim daftar baru setiap kali isi tabel berubah, jadi layar ikut berubah sendiri.
@Dao
interface AplikasiDibatasiDao {
    @Query("SELECT * FROM aplikasi_dibatasi")
    fun semua(): Flow<List<AplikasiDibatasi>>

    // Upsert = tambah kalau belum ada, perbarui kalau sudah ada.
    @Upsert
    suspend fun simpan(aplikasi: AplikasiDibatasi)

    @Query("DELETE FROM aplikasi_dibatasi WHERE paket = :paket")
    suspend fun hapus(paket: String)
}

// Satu baris = satu PDF materi. File PDF-nya disalin ke penyimpanan Tempora,
// dan namaFile adalah nama salinan itu.
@Entity(tableName = "materi")
data class Materi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val judul: String,
    val namaFile: String,
    val jumlahHalaman: Int,
    // Waktu ditambahkan, dalam milidetik sejak 1 Januari 1970 (format waktu bawaan Android).
    val ditambahkanPada: Long,
    // Rangkuman buatan AI. null berarti belum dibuat.
    val rangkuman: String? = null,
)

@Dao
interface MateriDao {
    @Query("SELECT * FROM materi ORDER BY ditambahkanPada DESC")
    fun semua(): Flow<List<Materi>>

    @Query("SELECT * FROM materi WHERE id = :id")
    suspend fun cari(id: Long): Materi?

    @Insert
    suspend fun tambah(materi: Materi): Long

    @Query("UPDATE materi SET rangkuman = :rangkuman WHERE id = :id")
    suspend fun simpanRangkuman(id: Long, rangkuman: String)

    @Delete
    suspend fun hapus(materi: Materi)
}

// Satu baris = satu soal pilihan ganda buatan AI untuk sebuah materi.
// ForeignKey + CASCADE: kalau materinya dihapus, semua soalnya ikut terhapus.
// indeksBenar adalah urutan jawaban benar, dihitung dari 0 (0 = A, 1 = B, 2 = C, 3 = D).
@Entity(
    tableName = "soal",
    foreignKeys = [
        ForeignKey(
            entity = Materi::class,
            parentColumns = ["id"],
            childColumns = ["idMateri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idMateri")],
)
data class SoalMateri(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idMateri: Long,
    val pertanyaan: String,
    val pilihanA: String,
    val pilihanB: String,
    val pilihanC: String,
    val pilihanD: String,
    val indeksBenar: Int,
    val penjelasan: String,
)

// Hasil hitungan: berapa soal yang dimiliki satu materi.
data class JumlahSoal(val idMateri: Long, val jumlah: Int)

@Dao
interface SoalDao {
    @Query("SELECT * FROM soal WHERE idMateri = :idMateri")
    suspend fun untuk(idMateri: Long): List<SoalMateri>

    @Query("SELECT idMateri, COUNT(*) AS jumlah FROM soal GROUP BY idMateri")
    fun jumlahPerMateri(): Flow<List<JumlahSoal>>

    @Insert
    suspend fun tambah(soal: List<SoalMateri>)

    @Query("DELETE FROM soal WHERE idMateri = :idMateri")
    suspend fun hapusUntuk(idMateri: Long)

    // Transaction: menghapus soal lama dan menyimpan soal baru terjadi sekaligus,
    // jadi tidak pernah ada keadaan setengah jadi.
    @Transaction
    suspend fun ganti(idMateri: Long, soal: List<SoalMateri>) {
        hapusUntuk(idMateri)
        tambah(soal)
    }
}

// Satu baris = satu soal yang dijawab di layar kuis, benar ataupun salah.
// Dipakai halaman Progres untuk menghitung soal yang dijawab per hari.
@Entity(tableName = "jawaban")
data class Jawaban(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Waktu menjawab, dalam milidetik sejak 1 Januari 1970.
    val dijawabPada: Long,
    val benar: Boolean,
)

@Dao
interface JawabanDao {
    @Insert
    suspend fun catat(jawaban: Jawaban)

    // Hanya waktunya yang diambil; pengelompokan per hari dilakukan di ProgresViewModel.
    @Query("SELECT dijawabPada FROM jawaban")
    fun semuaWaktu(): Flow<List<Long>>
}

@Database(
    entities = [AplikasiDibatasi::class, Materi::class, SoalMateri::class, Jawaban::class],
    version = 3,
    exportSchema = false,
)
abstract class TemporaDatabase : RoomDatabase() {
    abstract fun aplikasiDibatasiDao(): AplikasiDibatasiDao
    abstract fun materiDao(): MateriDao
    abstract fun soalDao(): SoalDao
    abstract fun jawabanDao(): JawabanDao

    companion object {
        fun buat(context: Context): TemporaDatabase =
            Room.databaseBuilder(context, TemporaDatabase::class.java, "tempora.db")
                .addMigrations(MIGRASI_1_2, MIGRASI_2_3)
                // Kalau suatu saat susunan tabel berubah tanpa dibuatkan migrasi,
                // data lama dihapus daripada aplikasi crash saat dibuka.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}

// Migrasi = cara mengubah database versi lama di HP menjadi versi baru tanpa menghapus isinya.
// Versi 2 menambah kolom rangkuman di tabel materi dan tabel soal. Perintah pembuatan tabel
// harus sama persis dengan yang dibuat Room, jadi jangan diubah.
private val MIGRASI_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `materi` ADD COLUMN `rangkuman` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `soal` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`idMateri` INTEGER NOT NULL, `pertanyaan` TEXT NOT NULL, `pilihanA` TEXT NOT NULL, " +
                "`pilihanB` TEXT NOT NULL, `pilihanC` TEXT NOT NULL, `pilihanD` TEXT NOT NULL, " +
                "`indeksBenar` INTEGER NOT NULL, `penjelasan` TEXT NOT NULL, " +
                "FOREIGN KEY(`idMateri`) REFERENCES `materi`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_soal_idMateri` ON `soal` (`idMateri`)")
    }
}

// Versi 3 menambah tabel jawaban.
private val MIGRASI_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `jawaban` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dijawabPada` INTEGER NOT NULL, `benar` INTEGER NOT NULL)",
        )
    }
}
