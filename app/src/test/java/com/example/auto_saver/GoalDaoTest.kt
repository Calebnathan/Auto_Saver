package com.example.auto_saver

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoalDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var goalDao: GoalDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        goalDao = db.goalDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndQueryGoalByMonth() = runBlocking {
        val userId = userDao.insert(User(loginId = "user3", password = "pass")).toInt()
        val month = "2025-10"
        val goal = Goal(userId = userId, month = month, minGoal = 100.0, maxGoal = 500.0)
        goalDao.insertGoal(goal)

        val fetched = goalDao.getGoalForMonth(userId, month)
        val aliasFetched = goalDao.getGoalByMonth(userId, month)

        assertThat(fetched).isNotNull()
        assertThat(aliasFetched).isNotNull()
        assertThat(fetched!!.maxGoal).isWithin(1e-6).of(500.0)
    }
}
