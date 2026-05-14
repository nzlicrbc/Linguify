package com.example.linguify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.linguify.model.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    // CEFR seviyesine göre rastgele sıralanmış kelimeleri getir
    @Query("SELECT * FROM words WHERE cefrLevel IN (:cefrLevels) ORDER BY RANDOM() LIMIT :limit OFFSET :offset")
    suspend fun getWordsByCefrLevels(cefrLevels: List<String>, limit: Int, offset: Int): List<WordEntity>

    // Belirli bir UserLevel için uygun CEFR seviyeleri içeren kelimeleri say
    @Query("SELECT COUNT(*) FROM words WHERE cefrLevel IN (:cefrLevels)")
    suspend fun getWordCountByCefrLevels(cefrLevels: List<String>): Int

    // Kelimeleri ekle
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    // Tüm kelimeleri sil
    @Query("DELETE FROM words")
    suspend fun deleteAllWords()

    // ID'ye göre kelime getir
    @Query("SELECT * FROM words WHERE id IN (:wordIds)")
    suspend fun getWordsByIds(wordIds: List<String>): List<WordEntity>

    // Kelime çevirisini güncelle
    @Query("UPDATE words SET translation = :translation, definition = :definition, example = :example, pronunciationUrl = :pronunciationUrl WHERE id = :wordId")
    suspend fun updateWordDetails(wordId: String, translation: String?, definition: String?, example: String?, pronunciationUrl: String?)

    // Her set için tamamen rastgele kelimeler getir
    @Query("SELECT * FROM words WHERE cefrLevel IN (:cefrLevels) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByCefrLevels(cefrLevels: List<String>, limit: Int): List<WordEntity>

    // Belirli ID'lere sahip kelimeler dışındaki kelimeleri getir
    @Query("SELECT * FROM words WHERE cefrLevel IN (:cefrLevels) AND id NOT IN (:excludeIds) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByCefrLevelsExcludingIds(cefrLevels: List<String>, limit: Int, excludeIds: List<String>): List<WordEntity>

    @Query("SELECT * FROM words WHERE text = :text LIMIT 1")
    suspend fun getWordByText(text: String): WordEntity?

    @Transaction
    suspend fun replaceAllWords(words: List<WordEntity>) {
        deleteAllWords()
        insertWords(words)
    }
}