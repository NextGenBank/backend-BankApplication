Feature: ATM Operations

  Scenario: Successful deposit into own account
    Given пользователь "ivan" с IBAN "NL123" имеет баланс 1000
    When он делает депозит 500 в "NL123"
    Then операция проходит успешно и баланс становится 1500

  Scenario: Withdraw with insufficient funds
    Given пользователь "ivan" с IBAN "NL456" имеет баланс 100
    When он пытается снять 200
    Then операция завершается ошибкой "Insufficient funds"
