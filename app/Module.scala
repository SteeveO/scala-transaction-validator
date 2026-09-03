import com.google.inject.{AbstractModule, Provides}
import domain.repositories.{AccountRepository, TransactionRepository}
import domain.services.ValidationEngine
import infrastructure.repositories.{InMemoryAccountRepository, InMemoryTransactionRepository}

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AccountRepository]).to(classOf[InMemoryAccountRepository])
    bind(classOf[TransactionRepository]).to(classOf[InMemoryTransactionRepository])
  }

  @Provides
  def provideValidationEngine(
      accountRepository: AccountRepository,
      transactionRepository: TransactionRepository
  ): ValidationEngine = new ValidationEngine(accountRepository, transactionRepository)
}
