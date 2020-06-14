package iskra.form

case class Field(progName: String = "",
                 name: String = "",
                 minlen: Int = -1,
                 maxlen: Int = -1,
                 tpe: String = "",
                 default: String = "",
                 sqlCombo: String = "",
                 key: String = "",
                 inForm: String = "",
                 regexp: String = "",
                 regexpError: String = "",
                 refTable: String = "",
                 refCol: String = ""
                ) {
  def isBoolType: Boolean = (tpe == "bit" || tpe == "bool")
}


