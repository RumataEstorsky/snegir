import iskra.form.{EasyForm, Field}

class EditGroup extends EasyForm {
  val fields = Map(
    "group" -> Field(
      progName = "Название группы",
      name = "group",
      minlen = 1,
      maxlen = 45,
      tpe = "varchar",
    ),
    "parent_group_id" -> Field(
      progName = "Вышестоящая группа",
      name = "parent_group_id",
      minlen = 0,
      maxlen = 10,
      tpe = "int",
      default = "29",
      sqlCombo = "SELECT `group_id`,`group` FROM `groups` WHERE `organization`",
    ),
    "organization" -> Field(
      progName = "Это организация",
      name = "organization",
      tpe = "bit",
    )
  )
  val _header_insert = "Добавление новой группы в базу:"
  val _header_update = "Редактирование группы:"
  val _act_name = "group_editor"
  val _table_name = "groups"
  val _id_field_name = "group_id"


  def afterLoad() {}

  def validateConcret() {
    //  if (mb_strtoupper(trim($_REQUEST["group"])) == "АДМИНИСТРАТОРЫ")
    //    self :: addError(Style :: validation(sprintf("Группу с названием " % s" создать нельзя", $_REQUEST["group"])), "group")
  }

  def afterValidateBeforeSave() {
    //  if ($_REQUEST["organization"] == "on") self :: $_result["organization"] = 1
    //  else self :: $_result["organization"] = 0
  }

  def afterSave() {
    //  // если это организация, то добавим группу "Преподаватели"
    //  if ($_REQUEST["organization"] == "on") {
    //    $g["group"] = "Преподаватели"
    //    $g["parent_group_id"] = mysql_insert_id()
    //    SQL :: exec(SQL :: insertString("groups", $g))
    //  }
    //  header("Location: index.php?act=group_editor")
  }

}