package com.jericho2code.app_finance_manager.application.di.modules

import com.jericho2code.app_finance_manager.application.di.Name
import com.jericho2code.app_finance_manager.model.database.AppDatabase
import com.jericho2code.app_finance_manager.model.database.dao.TemplateDao
import com.jericho2code.app_finance_manager.model.database.dao.TransactionDao
import com.jericho2code.app_finance_manager.model.repositories.TemplateRepository
import com.jericho2code.app_finance_manager.model.repositories.TransactionRepository
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import javax.inject.Named

@Module
class TemplateModule {
    @Provides
    fun provideRepository(
        dao: TemplateDao,
        @Named(Name.UI_SCHEDULER)
        uiScheduler: Scheduler,
        @Named(Name.IO_SCHEDULER)
        ioScheduler: Scheduler
    ): TemplateRepository = TemplateRepository(dao, uiScheduler, ioScheduler)

    @Provides
    fun provideDao(db: AppDatabase): TemplateDao = db.templateDao()
}