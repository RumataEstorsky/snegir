package iskra.form

import views.html.iskra.form._

trait EasyForm {
//  val _button_names["submit"] = "Сохранить"
//  val _button_names["reset"] = "Очистить"
//  def _errors: Map[String, Seq[String]]
//  def _result
//  def _stored
//  def _arr
  def _table_name: String
  def _id_field_name: String
//  def _edit_id = -1
//  def $_input_hiddens: Seq[(String, String)]
  def _header_insert: String
  def _header_update: String
  def _act_name: String
//  def _button_names: String


  def formRow(f: Field): Unit =
    if(f.inForm.nonEmpty) f.inForm
    else if(f.sqlCombo.nonEmpty) sqlCombo(f)
         else if(f.maxlen > 100) textArea(f)
              else if(f.isBoolType) checkBox(f, "1") //TODO
              else textInput(f, "1")  //TODO

}
