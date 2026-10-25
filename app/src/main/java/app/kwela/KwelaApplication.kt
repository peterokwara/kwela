package app.kwela

import android.app.Application
import androidx.room.Room
import app.kwela.data.KwelaRepository
import app.kwela.data.notify.ReplyNotifications
import app.kwela.data.local.KwelaDatabase
import app.kwela.data.remote.KwelaApi

class KwelaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReplyNotifications(this).ensureChannel()
    }

    val database: KwelaDatabase by lazy {
        Room.databaseBuilder(this, KwelaDatabase::class.java, "kwela.db").addMigrations(KwelaDatabase.MIGRATION_1_2, KwelaDatabase.MIGRATION_2_3, KwelaDatabase.MIGRATION_3_4).build()
    }

    val repository: KwelaRepository by lazy { KwelaRepository(this, database) }

    val api: KwelaApi by lazy { KwelaApi(BuildConfig.API_BASE_URL) }
}
