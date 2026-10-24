package app.kwela

import android.app.Application
import androidx.room.Room
import app.kwela.data.KwelaRepository
import app.kwela.data.local.KwelaDatabase

class KwelaApplication : Application() {
    val database: KwelaDatabase by lazy {
        Room.databaseBuilder(this, KwelaDatabase::class.java, "kwela.db").build()
    }

    val repository: KwelaRepository by lazy { KwelaRepository(this, database) }
}
