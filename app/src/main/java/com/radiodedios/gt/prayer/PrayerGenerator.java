package com.radiodedios.gt.prayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PrayerGenerator {

    private static final String PREF_NAME = "prayer_history_prefs";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_LAST_PRAYER_INDEX = "last_prayer_index";

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Random random;

    public PrayerGenerator(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.random = new Random();
    }

    public Prayer generatePrayer(String name, String category, String description) {
        String[] templates = getTemplatesForCategory(category);
        if (templates == null || templates.length == 0) {
            templates = new String[] { "Señor, te pedimos por %s en su situación: %s. Amén." };
        }

        int lastIndex = prefs.getInt(KEY_LAST_PRAYER_INDEX + "_" + category, -1);
        int newIndex;
        if (templates.length > 1) {
            do {
                newIndex = random.nextInt(templates.length);
            } while (newIndex == lastIndex);
        } else {
            newIndex = 0;
        }

        prefs.edit().putInt(KEY_LAST_PRAYER_INDEX + "_" + category, newIndex).apply();

        String template = templates[newIndex];

        String cleanName = (name != null && !name.trim().isEmpty()) ? name.trim() : "esta persona";
        String cleanDesc = (description != null && !description.trim().isEmpty()) ? description.trim() : "lo que atraviesa";

        String prayerText = String.format(template, cleanName, cleanDesc);

        String verse = getRandomVerseForCategory(category);

        Prayer prayer = new Prayer(
                System.currentTimeMillis(),
                category,
                prayerText,
                verse
        );

        saveToHistory(prayer);
        return prayer;
    }

    private String[] getTemplatesForCategory(String category) {
        // En un caso real, estas plantillas vendrían de strings.xml o una BD local
        // Usaremos unas hardcodeadas por simplicidad que soporten %s (nombre) y %s (descripción)
        switch (category.toLowerCase()) {
            case "salud":
                return new String[]{
                        "Señor Jesucristo, médico divino, te presento a %s. Pon tu mano sanadora sobre %s y restaura su salud física y emocional. Que tu voluntad perfecta se cumpla en su vida. Amén.",
                        "Padre amado, te ruego por la salud de %s. Tú conoces su dolor y su situación: %s. Dale fortaleza y permite que recupere su bienestar pronto. Amén.",
                        "Dios de infinita misericordia, te pedimos por %s. Trae sanidad y consuelo frente a %s. Que tu paz que sobrepasa todo entendimiento guarde su corazón. Amén."
                };
            case "economía":
            case "economia":
                return new String[]{
                        "Padre Proveedor, abrimos nuestro corazón por %s. En medio de esta necesidad: %s, abre las ventanas de los cielos y derrama bendición hasta que sobreabunde. Amén.",
                        "Señor, dueño del oro y la plata, te encomiendo las finanzas de %s. Ante %s, danos sabiduría y abre puertas de oportunidad que ningún hombre pueda cerrar. Amén.",
                        "Dios fiel, te pedimos por la situación económica de %s. Que %s sea una prueba temporal de la cual salgan fortalecidos, confiando siempre en tu provisión. Amén."
                };
            case "familia":
                return new String[]{
                        "Señor, te presentamos a la familia de %s. En medio de %s, trae unidad, perdón y amor incondicional a su hogar. Que tu Espíritu Santo sea el centro de sus vidas. Amén.",
                        "Padre Celestial, te ruego por %s y sus seres queridos. Fortalece sus lazos familiares frente a %s. Que el amor y la comprensión reinen siempre en su casa. Amén.",
                        "Dios de paz, bendice el hogar de %s. Ante la situación de %s, te pedimos que restaures las relaciones rotas y traigas armonía a su familia. Amén."
                };
            case "ansiedad":
                return new String[]{
                        "Príncipe de Paz, calma la mente y el corazón de %s. En este momento de %s, disipa todo temor y angustia. Llena su ser con tu dulce presencia y dales descanso. Amén.",
                        "Señor, te entrego la ansiedad de %s. Conociendo que %s les aflige, te pedimos que echen toda su ansiedad sobre Ti, porque Tú cuidas de ellos. Amén.",
                        "Padre de consuelo, abraza a %s con tu amor. Frente a %s, te ruego que tu paz, que sobrepasa todo entendimiento, guarde su corazón y sus pensamientos en Cristo Jesús. Amén."
                };
            case "fortaleza espiritual":
            case "fortaleza":
                return new String[]{
                        "Espíritu Santo, renueva las fuerzas de %s. En medio de %s, levanta su fe como las águilas, para que corran y no se cansen, caminen y no se fatiguen. Amén.",
                        "Señor, fortalece el espíritu de %s. Ante la prueba de %s, te pedimos que se mantengan firmes en tu Palabra y confíen plenamente en tus promesas. Amén.",
                        "Padre Amado, derrama tu poder sobre %s. Que %s no sea motivo de desánimo, sino una oportunidad para ver tu gloria y crecer espiritualmente. Amén."
                };
            default: // General
                return new String[]{
                        "Señor, elevamos esta oración por %s. Tú conoces profundamente: %s. Te pedimos que obres según tu perfecta voluntad y derrames tus bendiciones. Amén.",
                        "Padre Nuestro, te entregamos a %s. En esta situación: %s, te rogamos que guíes sus pasos y les llenes de tu gracia inagotable. Amén.",
                        "Dios Todopoderoso, escucha nuestro clamor por %s. Ante %s, muestra tu gran poder y misericordia, trayendo paz y solución a sus vidas. Amén."
                };
        }
    }

    private String getRandomVerseForCategory(String category) {
        String[] verses;
        switch (category.toLowerCase()) {
            case "salud":
                verses = new String[]{
                        "Mas yo haré venir sanidad para ti, y sanaré tus heridas, dice Jehová. - Jeremías 30:17",
                        "Él es quien perdona todas tus iniquidades, El que sana todas tus dolencias. - Salmos 103:3",
                        "Sáname, oh Jehová, y seré sano; sálvame, y seré salvo; porque tú eres mi alabanza. - Jeremías 17:14"
                };
                break;
            case "economía":
            case "economia":
                verses = new String[]{
                        "Mi Dios, pues, suplirá todo lo que os falta conforme a sus riquezas en gloria en Cristo Jesús. - Filipenses 4:19",
                        "Jehová es mi pastor; nada me faltará. - Salmos 23:1",
                        "Joven fui, y he envejecido, Y no he visto justo desamparado, Ni su descendencia que mendigue pan. - Salmos 37:25"
                };
                break;
            case "familia":
                verses = new String[]{
                        "Cree en el Señor Jesucristo, y serás salvo, tú y tu casa. - Hechos 16:31",
                        "Y nosotros hemos conocido y creído el amor que Dios tiene para con nosotros. Dios es amor... - 1 Juan 4:16",
                        "Mirad cuán bueno y cuán delicioso es Habitar los hermanos juntos en armonía! - Salmos 133:1"
                };
                break;
            case "ansiedad":
                verses = new String[]{
                        "Echando toda vuestra ansiedad sobre él, porque él tiene cuidado de vosotros. - 1 Pedro 5:7",
                        "Por nada estéis afanosos, sino sean conocidas vuestras peticiones delante de Dios en toda oración y ruego... - Filipenses 4:6",
                        "La paz os dejo, mi paz os doy; yo no os la doy como el mundo la da. No se turbe vuestro corazón, ni tenga miedo. - Juan 14:27"
                };
                break;
            case "fortaleza espiritual":
            case "fortaleza":
                verses = new String[]{
                        "Todo lo puedo en Cristo que me fortalece. - Filipenses 4:13",
                        "Pero los que esperan a Jehová tendrán nuevas fuerzas; levantarán alas como las águilas... - Isaías 40:31",
                        "Mira que te mando que te esfuerces y seas valiente; no temas ni desmayes, porque Jehová tu Dios estará contigo... - Josué 1:9"
                };
                break;
            default:
                verses = new String[]{
                        "Clama a mí, y yo te responderé, y te enseñaré cosas grandes y ocultas que tú no conoces. - Jeremías 33:3",
                        "Confía en Jehová con todo tu corazón, Y no te apoyes en tu propia prudencia. - Proverbios 3:5",
                        "Porque yo sé los pensamientos que tengo acerca de vosotros, dice Jehová, pensamientos de paz, y no de mal... - Jeremías 29:11"
                };
                break;
        }
        return verses[random.nextInt(verses.length)];
    }

    public void saveToHistory(Prayer prayer) {
        List<Prayer> history = getHistory();
        history.add(0, prayer); // Add to beginning
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    public List<Prayer> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<Prayer>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
