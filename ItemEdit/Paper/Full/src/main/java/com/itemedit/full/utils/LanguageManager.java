package com.itemedit.full.utils;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static final Map<String, Map<String, String>> translations = new HashMap<>();

    static {
        // English
        Map<String, String> en = new HashMap<>();
        en.put("no_player", "&cOnly players can use this command.");
        en.put("no_permission", "&cYou do not have permission to use this command.");
        en.put("hold_item", "&cYou must hold an item in your main hand.");
        en.put("usage_rename", "&cUsage: /ie rename <name>");
        en.put("rename_success", "&aItem renamed successfully!");
        en.put("usage_lore", "&cUsage: /ie lore <add/set/remove/clear> [args]");
        en.put("usage_lore_add", "&cUsage: /ie lore add <text>");
        en.put("lore_add_success", "&aAdded lore line.");
        en.put("usage_lore_set", "&cUsage: /ie lore set <line> <text>");
        en.put("lore_invalid_line", "&cInvalid line number. Current size is %size%.");
        en.put("lore_set_success", "&aSet lore line %line%.");
        en.put("line_must_be_number", "&cLine must be a number.");
        en.put("usage_lore_remove", "&cUsage: /ie lore remove <line>");
        en.put("lore_out_of_bounds", "&cLine number out of bounds.");
        en.put("lore_remove_success", "&aRemoved lore line %line%.");
        en.put("lore_clear_success", "&aLore cleared.");
        en.put("unknown_operation", "&cUnknown operation. Use add, set, remove, or clear.");
        en.put("usage_enchant", "&cUsage: /ie enchant <enchantment> <level>");
        en.put("level_must_be_number", "&cLevel must be a number.");
        en.put("enchant_not_found", "&cEnchantment not found.");
        en.put("enchant_remove_success", "&aRemoved enchantment %enchant%.");
        en.put("enchant_add_success", "&aAdded enchantment %enchant% Level %level%.");
        en.put("usage_unbreakable", "&cUsage: /ie unbreakable <true/false>");
        en.put("unbreakable_success", "&aSet unbreakable to %state%.");
        en.put("usage_flag", "&cUsage: /ie flag <add/remove/clear> [flag]");
        en.put("flag_clear_success", "&aCleared all item flags.");
        en.put("usage_flag_op", "&cUsage: /ie flag <add/remove> <flag>");
        en.put("invalid_flag", "&cInvalid item flag.");
        en.put("flag_add_success", "&aAdded flag %flag%.");
        en.put("flag_remove_success", "&aRemoved flag %flag%.");
        en.put("usage_attribute", "&cUsage: /ie attribute <add/remove/clear> [attribute] [value]");
        en.put("attribute_clear_success", "&aCleared all attributes.");
        en.put("usage_attribute_op", "&cUsage: /ie attribute <add/remove> <attribute> [value]");
        en.put("invalid_attribute", "&cInvalid attribute.");
        en.put("usage_attribute_add", "&cUsage: /ie attribute add <attribute> <value>");
        en.put("value_must_be_number", "&cValue must be a number.");
        en.put("attribute_add_success", "&aAdded attribute %attr% with value %val%.");
        en.put("attribute_remove_success", "&aRemoved attribute %attr%.");
        en.put("hidetooltips_success", "&aSet hide tooltips to %state%.");
        en.put("usage_ability", "&cUsage: /ie ability <add/remove/clear/list> [ability]");
        en.put("ability_list_header", "&6&lAvailable Abilities:");
        en.put("ability_clear_success", "&aCleared all abilities from your item.");
        en.put("usage_ability_op", "&cUsage: /ie ability <add/remove> <ability_id>");
        en.put("ability_not_exist", "&cAbility '%ability%' does not exist.");
        en.put("ability_already_has", "&cThis item already has this ability.");
        en.put("ability_add_success", "&aAdded ability '%ability%' to your item.");
        en.put("ability_not_has", "&cThis item does not have this ability.");
        en.put("ability_remove_success", "&aRemoved ability '%ability%' from your item.");
        en.put("ability_unknown_op", "&cUnknown operation. Use add, remove, or list.");
        
        // Paid Extra
        en.put("no_permission_reload", "&cYou do not have permission to reload the configuration.");
        en.put("reload_success", "&aItemEdit configuration and weapon.yml successfully reloaded!");
        en.put("usage_give", "&cUsage: /ie give [player] <weapon_key>");
        en.put("give_not_found", "&cWeapon key '%weapon%' not found in weapon.yml.");
        en.put("give_success", "&aGave %weapon% to %player%.");
        en.put("player_not_found", "&cPlayer not found.");
        en.put("usage_custom", "&cUsage: /ie custom <ability> <param> <value>");
        en.put("custom_success", "&aSet parameter %param% of ability %ability% to %val%.");
        translations.put("en", en);

        // Spanish (es)
        Map<String, String> es = new HashMap<>();
        es.put("no_player", "&cSolo los jugadores pueden usar este comando.");
        es.put("no_permission", "&cNo tienes permiso para usar este comando.");
        es.put("hold_item", "&cDebes sostener un objeto en tu mano principal.");
        es.put("usage_rename", "&cUso: /ie rename <nombre>");
        es.put("rename_success", "&a¡Objeto renombrado con éxito!");
        es.put("usage_lore", "&cUso: /ie lore <add/set/remove/clear> [args]");
        es.put("usage_lore_add", "&cUso: /ie lore add <texto>");
        es.put("lore_add_success", "&aLínea de lore añadida.");
        es.put("usage_lore_set", "&cUso: /ie lore set <línea> <texto>");
        es.put("lore_invalid_line", "&cNúmero de línea inválido. El tamaño actual es %size%.");
        es.put("lore_set_success", "&aLínea de lore %line% establecida.");
        es.put("line_must_be_number", "&cLa línea debe ser un número.");
        es.put("usage_lore_remove", "&cUso: /ie lore remove <línea>");
        es.put("lore_out_of_bounds", "&cNúmero de línea fuera de los límites.");
        es.put("lore_remove_success", "&aLínea de lore %line% eliminada.");
        es.put("lore_clear_success", "&aLore limpiado.");
        es.put("unknown_operation", "&cOperación desconocida. Usa add, set, remove o clear.");
        es.put("usage_enchant", "&cUso: /ie enchant <encantamiento> <nivel>");
        es.put("level_must_be_number", "&cEl nivel debe ser un número.");
        es.put("enchant_not_found", "&cEncantamiento no encontrado.");
        es.put("enchant_remove_success", "&aEncantamiento %enchant% eliminado.");
        es.put("enchant_add_success", "&aEncantamiento %enchant% Nivel %level% añadido.");
        es.put("usage_unbreakable", "&cUso: /ie unbreakable <true/false>");
        es.put("unbreakable_success", "&aIrrompible establecido a %state%.");
        es.put("usage_flag", "&cUso: /ie flag <add/remove/clear> [flag]");
        es.put("flag_clear_success", "&aTodos los flags del objeto han sido limpiados.");
        es.put("usage_flag_op", "&cUso: /ie flag <add/remove> <flag>");
        es.put("invalid_flag", "&cFlag de objeto inválido.");
        es.put("flag_add_success", "&aFlag %flag% añadido.");
        es.put("flag_remove_success", "&aFlag %flag% eliminado.");
        es.put("usage_attribute", "&cUso: /ie attribute <add/remove/clear> [atributo] [valor]");
        es.put("attribute_clear_success", "&aTodos los atributos han sido limpiados.");
        es.put("usage_attribute_op", "&cUso: /ie attribute <add/remove> <atributo> [valor]");
        es.put("invalid_attribute", "&cAtributo inválido.");
        es.put("usage_attribute_add", "&cUso: /ie attribute add <atributo> <valor>");
        es.put("value_must_be_number", "&cEl valor debe ser un número.");
        es.put("attribute_add_success", "&aAtributo %attr% con valor %val% añadido.");
        es.put("attribute_remove_success", "&aAtributo %attr% eliminado.");
        es.put("hidetooltips_success", "&aOcultar descripciones establecido a %state%.");
        es.put("usage_ability", "&cUso: /ie ability <add/remove/clear/list> [habilidad]");
        es.put("ability_list_header", "&6&lHabilidades Disponibles:");
        es.put("ability_clear_success", "&aTodas las habilidades eliminadas de tu objeto.");
        es.put("usage_ability_op", "&cUso: /ie ability <add/remove> <id_habilidad>");
        es.put("ability_not_exist", "&cLa habilidad '%ability%' no existe.");
        es.put("ability_already_has", "&cEste objeto ya tiene esta habilidad.");
        es.put("ability_add_success", "&aHabilidad '%ability%' añadida a tu objeto.");
        es.put("ability_not_has", "&cEste objeto no tiene esta habilidad.");
        es.put("ability_remove_success", "&aHabilidad '%ability%' eliminada de tu objeto.");
        es.put("ability_unknown_op", "&cOperación desconocida. Usa add, remove o list.");
        
        // Paid Extra
        es.put("no_permission_reload", "&cNo tienes permiso para recargar la configuración.");
        es.put("reload_success", "&a¡Configuración de ItemEdit y weapon.yml recargadas con éxito!");
        es.put("usage_give", "&cUso: /ie give [jugador] <clave_arma>");
        es.put("give_not_found", "&cClave de arma '%weapon%' no encontrada en weapon.yml.");
        es.put("give_success", "&aSe entregó %weapon% a %player%.");
        es.put("player_not_found", "&cJugador no encontrado.");
        es.put("usage_custom", "&cUso: /ie custom <habilidad> <parámetro> <valor>");
        es.put("custom_success", "&aEstablecido el parámetro %param% de la habilidad %ability% a %val%.");
        translations.put("es", es);

        // French (fr)
        Map<String, String> fr = new HashMap<>();
        fr.put("no_player", "&cSeuls les joueurs peuvent utiliser cette commande.");
        fr.put("no_permission", "&cVous n'avez pas la permission d'utiliser cette commande.");
        fr.put("hold_item", "&cVous devez tenir un objet dans votre main principale.");
        fr.put("usage_rename", "&cUsage: /ie rename <nom>");
        fr.put("rename_success", "&aObjet renommé avec succès !");
        fr.put("usage_lore", "&cUsage: /ie lore <add/set/remove/clear> [args]");
        fr.put("usage_lore_add", "&cUsage: /ie lore add <texte>");
        fr.put("lore_add_success", "&aLigne de description ajoutée.");
        fr.put("usage_lore_set", "&cUsage: /ie lore set <ligne> <texte>");
        fr.put("lore_invalid_line", "&cLigne invalide. La taille actuelle est %size%.");
        fr.put("lore_set_success", "&aLigne de description %line% définie.");
        fr.put("line_must_be_number", "&cLa ligne doit être un nombre.");
        fr.put("usage_lore_remove", "&cUsage: /ie lore remove <ligne>");
        fr.put("lore_out_of_bounds", "&cNuméro de ligne hors limites.");
        fr.put("lore_remove_success", "&aLigne de description %line% supprimée.");
        fr.put("lore_clear_success", "&aDescription effacée.");
        fr.put("unknown_operation", "&cOpération inconnue. Utilisez add, set, remove ou clear.");
        fr.put("usage_enchant", "&cUsage: /ie enchant <enchantement> <niveau>");
        fr.put("level_must_be_number", "&cLe niveau doit être un nombre.");
        fr.put("enchant_not_found", "&cEnchantement non trouvé.");
        fr.put("enchant_remove_success", "&aEnchantement %enchant% supprimé.");
        fr.put("enchant_add_success", "&aEnchantement %enchant% Niveau %level% ajouté.");
        fr.put("usage_unbreakable", "&cUsage: /ie unbreakable <true/false>");
        fr.put("unbreakable_success", "&aIncassable défini sur %state%.");
        fr.put("usage_flag", "&cUsage: /ie flag <add/remove/clear> [flag]");
        fr.put("flag_clear_success", "&aTous les flags d'objet ont été effacés.");
        fr.put("usage_flag_op", "&cUsage: /ie flag <add/remove> <flag>");
        fr.put("invalid_flag", "&cFlag d'objet invalide.");
        fr.put("flag_add_success", "&aFlag %flag% ajouté.");
        fr.put("flag_remove_success", "&aFlag %flag% supprimé.");
        fr.put("usage_attribute", "&cUsage: /ie attribute <add/remove/clear> [attribut] [valeur]");
        fr.put("attribute_clear_success", "&aTous les attributs ont été effacés.");
        fr.put("usage_attribute_op", "&cUsage: /ie attribute <add/remove> <attribut> [valeur]");
        fr.put("invalid_attribute", "&cAttribut invalide.");
        fr.put("usage_attribute_add", "&cUsage: /ie attribute add <attribut> <valeur>");
        fr.put("value_must_be_number", "&cLa valeur doit être un nombre.");
        fr.put("attribute_add_success", "&aAttribut %attr% avec la valeur %val% ajouté.");
        fr.put("attribute_remove_success", "&aAttribut %attr% supprimé.");
        fr.put("hidetooltips_success", "&aMasquage des info-bulles défini sur %state%.");
        fr.put("usage_ability", "&cUsage: /ie ability <add/remove/clear/list> [capacité]");
        fr.put("ability_list_header", "&6&lCapacités Disponibles :");
        fr.put("ability_clear_success", "&aToutes les capacités ont été retirées de votre objet.");
        fr.put("usage_ability_op", "&cUsage: /ie ability <add/remove> <id_capacité>");
        fr.put("ability_not_exist", "&cLa capacité '%ability%' n'existe pas.");
        fr.put("ability_already_has", "&cCet objet possède déjà cette capacité.");
        fr.put("ability_add_success", "&aCapacité '%ability%' ajoutée à votre objet.");
        fr.put("ability_not_has", "&cCet objet n'a pas cette capacité.");
        fr.put("ability_remove_success", "&aCapacité '%ability%' retirée de votre objet.");
        fr.put("ability_unknown_op", "&cOpération inconnue. Utilisez add, remove ou list.");
        
        // Paid Extra
        fr.put("no_permission_reload", "&cVous n'avez pas la permission de recharger la configuration.");
        fr.put("reload_success", "&aLa configuration d'ItemEdit et le fichier weapon.yml ont été rechargés avec succès !");
        fr.put("usage_give", "&cUsage: /ie give [joueur] <clé_arme>");
        fr.put("give_not_found", "&cLa clé d'arme '%weapon%' n'existe pas dans le fichier weapon.yml.");
        fr.put("give_success", "&a%weapon% a été donné à %player%.");
        fr.put("player_not_found", "&cJoueur non trouvé.");
        fr.put("usage_custom", "&cUsage: /ie custom <capacité> <paramètre> <valeur>");
        fr.put("custom_success", "&aLe paramètre %param% de la capacité %ability% a été défini sur %val%.");
        translations.put("fr", fr);

        // German (de)
        Map<String, String> de = new HashMap<>();
        de.put("no_player", "&cNur Spieler können diesen Befehl verwenden.");
        de.put("no_permission", "&cDu hast keine Berechtigung, diesen Befehl zu verwenden.");
        de.put("hold_item", "&cDu musst ein Item in deiner Haupthand halten.");
        de.put("usage_rename", "&cVerwendung: /ie rename <name>");
        de.put("rename_success", "&aItem erfolgreich umbenannt!");
        de.put("usage_lore", "&cVerwendung: /ie lore <add/set/remove/clear> [args]");
        de.put("usage_lore_add", "&cVerwendung: /ie lore add <text>");
        de.put("lore_add_success", "&aLore-Zeile hinzugefügt.");
        de.put("usage_lore_set", "&cVerwendung: /ie lore set <zeile> <text>");
        de.put("lore_invalid_line", "&cUngültige Zeilennummer. Die aktuelle Größe ist %size%.");
        de.put("lore_set_success", "&aLore-Zeile %line% gesetzt.");
        de.put("line_must_be_number", "&cZeile muss eine Nummer sein.");
        de.put("usage_lore_remove", "&cVerwendung: /ie lore remove <zeile>");
        de.put("lore_out_of_bounds", "&cZeilennummer außerhalb des gültigen Bereichs.");
        de.put("lore_remove_success", "&aLore-Zeile %line% entfernt.");
        de.put("lore_clear_success", "&aLore geleert.");
        de.put("unknown_operation", "&cUnbekannte Operation. Verwende add, set, remove oder clear.");
        de.put("usage_enchant", "&cVerwendung: /ie enchant <enchantment> <level>");
        de.put("level_must_be_number", "&cLevel muss eine Nummer sein.");
        de.put("enchant_not_found", "&cVerzauberung nicht gefunden.");
        de.put("enchant_remove_success", "&aVerzauberung %enchant% entfernt.");
        de.put("enchant_add_success", "&aVerzauberung %enchant% Level %level% hinzugefügt.");
        de.put("usage_unbreakable", "&cVerwendung: /ie unbreakable <true/false>");
        de.put("unbreakable_success", "&aUnzerbrechlich auf %state% gesetzt.");
        de.put("usage_flag", "&cVerwendung: /ie flag <add/remove/clear> [flag]");
        de.put("flag_clear_success", "&aAlle Item-Flags gelöscht.");
        de.put("usage_flag_op", "&cVerwendung: /ie flag <add/remove> <flag>");
        de.put("invalid_flag", "&cUngültiges Item-Flag.");
        de.put("flag_add_success", "&aFlag %flag% hinzugefügt.");
        de.put("flag_remove_success", "&aFlag %flag% entfernt.");
        de.put("usage_attribute", "&cVerwendung: /ie attribute <add/remove/clear> [attribut] [wert]");
        de.put("attribute_clear_success", "&aAlle Attribute gelöscht.");
        de.put("usage_attribute_op", "&cVerwendung: /ie attribute <add/remove> <attribut> [wert]");
        de.put("invalid_attribute", "&cUngültiges Attribut.");
        de.put("usage_attribute_add", "&cVerwendung: /ie attribute add <attribut> <wert>");
        de.put("value_must_be_number", "&cWert muss eine Nummer sein.");
        de.put("attribute_add_success", "&aAttribut %attr% mit Wert %val% hinzugefügt.");
        de.put("attribute_remove_success", "&aAttribut %attr% entfernt.");
        de.put("hidetooltips_success", "&aTooltips ausblenden auf %state% gesetzt.");
        de.put("usage_ability", "&cVerwendung: /ie ability <add/remove/clear/list> [fähigkeit]");
        de.put("ability_list_header", "&6&lVerfügbare Fähigkeiten:");
        de.put("ability_clear_success", "&aAlle Fähigkeiten von deinem Item gelöscht.");
        de.put("usage_ability_op", "&cVerwendung: /ie ability <add/remove> <fähigkeits_id>");
        de.put("ability_not_exist", "&cFähigkeit '%ability%' existiert nicht.");
        de.put("ability_already_has", "&cDieses Item hat bereits diese Fähigkeit.");
        de.put("ability_add_success", "&aFähigkeit '%ability%' zu deinem Item hinzugefügt.");
        de.put("ability_not_has", "&cDieses Item hat diese Fähigkeit nicht.");
        de.put("ability_remove_success", "&aFähigkeit '%ability%' von deinem Item entfernt.");
        de.put("ability_unknown_op", "&cUnbekannte Operation. Verwende add, remove oder list.");
        
        // Paid Extra
        de.put("no_permission_reload", "&cDu hast keine Berechtigung, die Konfiguration neu zu laden.");
        de.put("reload_success", "&aItemEdit-Konfiguration und weapon.yml erfolgreich neu geladen!");
        de.put("usage_give", "&cVerwendung: /ie give [spieler] <waffen_schlüssel>");
        de.put("give_not_found", "&cWaffenschlüssel '%weapon%' nicht in der weapon.yml gefunden.");
        de.put("give_success", "&a%weapon% an %player% gegeben.");
        de.put("player_not_found", "&cSpieler nicht gefunden.");
        de.put("usage_custom", "&cVerwendung: /ie custom <fähigkeit> <parameter> <wert>");
        de.put("custom_success", "&aParameter %param% der Fähigkeit %ability% auf %val% gesetzt.");
        translations.put("de", de);

        // Chinese (zh)
        Map<String, String> zh = new HashMap<>();
        zh.put("no_player", "&c只有玩家才能使用此命令。");
        zh.put("no_permission", "&c你没有权限使用此命令。");
        zh.put("hold_item", "&c你必须在主手中持有一个物品。");
        zh.put("usage_rename", "&c用法: /ie rename <名称>");
        zh.put("rename_success", "&a物品成功重命名！");
        zh.put("usage_lore", "&c用法: /ie lore <add/set/remove/clear> [参数]");
        zh.put("usage_lore_add", "&c用法: /ie lore add <文本>");
        zh.put("lore_add_success", "&a已添加Lore行。");
        zh.put("usage_lore_set", "&c用法: /ie lore set <行号> <文本>");
        zh.put("lore_invalid_line", "&c无效的行号。当前大小为 %size%。");
        zh.put("lore_set_success", "&a已设置第 %line% 行 Lore。");
        zh.put("line_must_be_number", "&c行号必须是数字。");
        zh.put("usage_lore_remove", "&c用法: /ie lore remove <行号>");
        zh.put("lore_out_of_bounds", "&c行号超出范围。");
        zh.put("lore_remove_success", "&a已移除第 %line% 行 Lore。");
        zh.put("lore_clear_success", "&aLore已清除。");
        zh.put("unknown_operation", "&c未知的操作。请使用 add、set、remove 或 clear。");
        zh.put("usage_enchant", "&c用法: /ie enchant <附魔> <等级>");
        zh.put("level_must_be_number", "&c等级必须是数字。");
        zh.put("enchant_not_found", "&c未找到该附魔。");
        zh.put("enchant_remove_success", "&a已移除附魔 %enchant%。");
        zh.put("enchant_add_success", "&a已添加附魔 %enchant% 等级 %level%。");
        zh.put("usage_unbreakable", "&c用法: /ie unbreakable <true/false>");
        zh.put("unbreakable_success", "&a无法破坏状态已设置为 %state%。");
        zh.put("usage_flag", "&c用法: /ie flag <add/remove/clear> [标签]");
        zh.put("flag_clear_success", "&a已清除所有物品标签。");
        zh.put("usage_flag_op", "&c用法: /ie flag <add/remove> <标签>");
        zh.put("invalid_flag", "&c无效的物品标签。");
        zh.put("flag_add_success", "&a已添加标签 %flag%。");
        zh.put("flag_remove_success", "&a已移除标签 %flag%。");
        zh.put("usage_attribute", "&c用法: /ie attribute <add/remove/clear> [属性] [数值]");
        zh.put("attribute_clear_success", "&a已清除所有属性。");
        zh.put("usage_attribute_op", "&c用法: /ie attribute <add/remove> <属性] [数值]");
        zh.put("invalid_attribute", "&c无效的属性。");
        zh.put("usage_attribute_add", "&c用法: /ie attribute add <属性> <数值>");
        zh.put("value_must_be_number", "&c数值必须是数字。");
        zh.put("attribute_add_success", "&a已添加属性 %attr% 数值为 %val%。");
        zh.put("attribute_remove_success", "&a已移除属性 %attr%。");
        zh.put("hidetooltips_success", "&a隐藏提示框已设置为 %state%。");
        zh.put("usage_ability", "&c用法: /ie ability <add/remove/clear/list> [技能]");
        zh.put("ability_list_header", "&6&l可用技能:");
        zh.put("ability_clear_success", "&a已清除物品上的所有技能。");
        zh.put("usage_ability_op", "&c用法: /ie ability <add/remove> <技能_id>");
        zh.put("ability_not_exist", "&c技能 '%ability%' 不存在。");
        zh.put("ability_already_has", "&c此物品已拥有该技能。");
        zh.put("ability_add_success", "&a已向物品添加技能 '%ability%'。");
        zh.put("ability_not_has", "&c此物品未拥有该技能。");
        zh.put("ability_remove_success", "&a已从物品移除技能 '%ability%'。");
        zh.put("ability_unknown_op", "&c未知的操作。请使用 add、remove 或 list。");
        
        // Paid Extra
        zh.put("no_permission_reload", "&c你没有权限重载配置文件。");
        zh.put("reload_success", "&aItemEdit 配置文件和 weapon.yml 重载成功！");
        zh.put("usage_give", "&c用法: /ie give [玩家] <武器_key>");
        zh.put("give_not_found", "&c在 weapon.yml 中未找到武器键 '%weapon%'。");
        zh.put("give_success", "&a已将 %weapon% 给予玩家 %player%。");
        zh.put("player_not_found", "&c未找到该玩家。");
        zh.put("usage_custom", "&c用法: /ie custom <技能> <参数> <数值>");
        zh.put("custom_success", "&a已将技能 %ability% 的参数 %param% 设置为 %val%。");
        translations.put("zh", zh);

        // Russian (ru)
        Map<String, String> ru = new HashMap<>();
        ru.put("no_player", "&cТолько игроки могут использовать эту команду.");
        ru.put("no_permission", "&cУ вас нет прав на использование этой команды.");
        ru.put("hold_item", "&cВы должны держать предмет в главной руке.");
        ru.put("usage_rename", "&cИспользование: /ie rename <имя>");
        ru.put("rename_success", "&aПредмет успешно переименован!");
        ru.put("usage_lore", "&cИспользование: /ie lore <add/set/remove/clear> [аргументы]");
        ru.put("usage_lore_add", "&cИспользование: /ie lore add <текст>");
        ru.put("lore_add_success", "&aСтрока описания добавлена.");
        ru.put("usage_lore_set", "&cИспользование: /ie lore set <строка> <текст>");
        ru.put("lore_invalid_line", "&cНеверный номер строки. Текущий размер: %size%.");
        ru.put("lore_set_success", "&aУстановлена строка описания %line%.");
        ru.put("line_must_be_number", "&cСтрока должна быть числом.");
        ru.put("usage_lore_remove", "&cИспользование: /ie lore remove <строка>");
        ru.put("lore_out_of_bounds", "&cНомер строки вышел за пределы.");
        ru.put("lore_remove_success", "&aУдалена строка описания %line%.");
        ru.put("lore_clear_success", "&aОписание очищено.");
        ru.put("unknown_operation", "&cНеизвестная операция. Используйте add, set, remove или clear.");
        ru.put("usage_enchant", "&cИспользование: /ie enchant <зачарование> <уровень>");
        ru.put("level_must_be_number", "&cУровень должен быть числом.");
        ru.put("enchant_not_found", "&cЗачарование не найдено.");
        ru.put("enchant_remove_success", "&aЗачарование %enchant% удалено.");
        ru.put("enchant_add_success", "&aДобавлено зачарование %enchant% уровня %level%.");
        ru.put("usage_unbreakable", "&cИспользование: /ie unbreakable <true/false>");
        ru.put("unbreakable_success", "&aНерушимость установлена на %state%.");
        ru.put("usage_flag", "&cИспользование: /ie flag <add/remove/clear> [флаг]");
        ru.put("flag_clear_success", "&aВсе флаги предмета очищены.");
        ru.put("usage_flag_op", "&cИспользование: /ie flag <add/remove> <флаг>");
        ru.put("invalid_flag", "&cНеверный флаг предмета.");
        ru.put("flag_add_success", "&aДобавлен флаг %flag%.");
        ru.put("flag_remove_success", "&aУдален флаг %flag%.");
        ru.put("usage_attribute", "&cИспользование: /ie attribute <add/remove/clear> [атрибут] [значение]");
        ru.put("attribute_clear_success", "&aВсе атрибуты очищены.");
        ru.put("usage_attribute_op", "&cИспользование: /ie attribute <add/remove> <атрибут> [значение]");
        ru.put("invalid_attribute", "&cНеверный атрибут.");
        ru.put("usage_attribute_add", "&cИспользование: /ie attribute add <атрибут> <значение>");
        ru.put("value_must_be_number", "&cЗначение должно быть числом.");
        ru.put("attribute_add_success", "&aДобавлен атрибут %attr% со значением %val%.");
        ru.put("attribute_remove_success", "&aАтрибут %attr% удален.");
        ru.put("hidetooltips_success", "&aСкрытие подсказок установлено на %state%.");
        ru.put("usage_ability", "&cИспользование: /ie ability <add/remove/clear/list> [способность]");
        ru.put("ability_list_header", "&6&lДоступные способности:");
        ru.put("ability_clear_success", "&aВсе способности удалены с вашего предмета.");
        ru.put("usage_ability_op", "&cИспользование: /ie ability <add/remove> <id_способности>");
        ru.put("ability_not_exist", "&cСпособность '%ability%' не существует.");
        ru.put("ability_already_has", "&cУ этого предмета уже есть эта способность.");
        ru.put("ability_add_success", "&aСпособность '%ability%' добавлена к вашему предмету.");
        ru.put("ability_not_has", "&cУ этого предмета нет этой способности.");
        ru.put("ability_remove_success", "&aСпособность '%ability%' удалена с вашего предмета.");
        ru.put("ability_unknown_op", "&cНеизвестная операция. Используйте add, remove или list.");
        
        // Paid Extra
        ru.put("no_permission_reload", "&cУ вас нет прав на перезагрузку конфигурации.");
        ru.put("reload_success", "&aКонфигурация ItemEdit и weapon.yml успешно перезагружены!");
        ru.put("usage_give", "&cИспользование: /ie give [игрок] <ключ_оружия>");
        ru.put("give_not_found", "&cКлюч оружия '%weapon%' не найден в weapon.yml.");
        ru.put("give_success", "&aВы дали %weapon% игроку %player%.");
        ru.put("player_not_found", "&cИгрок не найден.");
        ru.put("usage_custom", "&cИспользование: /ie custom <способность> <параметр> <значение>");
        ru.put("custom_success", "&aПараметр %param% способности %ability% изменен на %val%.");
        translations.put("ru", ru);
    }

    public static String getMessage(Player player, String key, Object... args) {
        String locale = player.getLocale().toLowerCase();
        String langCode = locale.split("_")[0];
        
        Map<String, String> langMap = translations.get(langCode);
        if (langMap == null) {
            langMap = translations.get("en");
        }
        
        String msg = langMap.get(key);
        if (msg == null) {
            msg = translations.get("en").get(key);
        }
        
        if (msg == null) {
            return "Missing translation key: " + key;
        }

        if (args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    msg = msg.replace(args[i].toString(), args[i + 1].toString());
                }
            }
        }
        
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
