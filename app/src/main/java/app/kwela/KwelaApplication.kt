package app.kwela

import android.app.Application
import androidx.room.Room
import app.kwela.data.KwelaRepository
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.remote.KwelaApi

class KwelaApplication : Application() {
    val database: KwelaDatabase by lazy {
        Room.databaseBuilder(this, KwelaDatabase::class.java, "kwela.db").build()
    }

    val repository: KwelaRepository by lazy { KwelaRepository(this, database) }

    val api: KwelaApi by lazy { KwelaApi(BuildConfig.API_BASE_URL) }
}
