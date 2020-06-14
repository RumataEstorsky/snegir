package util

object Russian {

  /** Пример PluralForm(101, 'яйцо', 'яйца', 'яиц') => 101 яйцо */
  def pluralForm(number: Int, form1: String, form2: String = "", form5: String) = {
    val n = Math.abs(number) % 100
    val n1 = n % 10
    if (n > 10 && n < 20) form5
    else if(n1 > 1 && n1 < 5) form2
    else if (n1 == 1) form1
    else form5
  }

}
