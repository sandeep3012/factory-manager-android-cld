package com.kulhad.manager.di

import android.content.Context
import com.kulhad.manager.data.local.KulhadDatabase
import com.kulhad.manager.data.local.dao.AttendanceDao
import com.kulhad.manager.data.local.dao.ExpenseDao
import com.kulhad.manager.data.local.dao.ExpenseTypeDao
import com.kulhad.manager.data.local.dao.PaymentDao
import com.kulhad.manager.data.local.dao.PieceRateDao
import com.kulhad.manager.data.local.dao.ProductDao
import com.kulhad.manager.data.local.dao.ProductionEntryDao
import com.kulhad.manager.data.local.dao.SaleDao
import com.kulhad.manager.data.local.dao.SaleItemDao
import com.kulhad.manager.data.local.dao.StockLedgerDao
import com.kulhad.manager.data.local.dao.UserDao
import com.kulhad.manager.data.local.dao.WorkerAdvanceDao
import com.kulhad.manager.data.local.dao.WorkerDao
import com.kulhad.manager.data.local.dao.WorkerTypeHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KulhadDatabase =
        KulhadDatabase.get(context)

    @Provides fun provideUserDao(db: KulhadDatabase): UserDao = db.userDao()
    @Provides fun provideWorkerDao(db: KulhadDatabase): WorkerDao = db.workerDao()
    @Provides fun provideWorkerTypeHistoryDao(db: KulhadDatabase): WorkerTypeHistoryDao =
        db.workerTypeHistoryDao()
    @Provides fun provideProductDao(db: KulhadDatabase): ProductDao = db.productDao()
    @Provides fun providePieceRateDao(db: KulhadDatabase): PieceRateDao = db.pieceRateDao()
    @Provides fun provideAttendanceDao(db: KulhadDatabase): AttendanceDao = db.attendanceDao()
    @Provides fun provideProductionEntryDao(db: KulhadDatabase): ProductionEntryDao =
        db.productionEntryDao()
    @Provides fun provideStockLedgerDao(db: KulhadDatabase): StockLedgerDao = db.stockLedgerDao()
    @Provides fun provideSaleDao(db: KulhadDatabase): SaleDao = db.saleDao()
    @Provides fun provideSaleItemDao(db: KulhadDatabase): SaleItemDao = db.saleItemDao()
    @Provides fun providePaymentDao(db: KulhadDatabase): PaymentDao = db.paymentDao()
    @Provides fun provideExpenseTypeDao(db: KulhadDatabase): ExpenseTypeDao = db.expenseTypeDao()
    @Provides fun provideExpenseDao(db: KulhadDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideWorkerAdvanceDao(db: KulhadDatabase): WorkerAdvanceDao =
        db.workerAdvanceDao()
}
