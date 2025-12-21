package com.example.educationalapp.alphabet

import com.example.educationalapp.R

data class AlphabetItem(
    val baseLetter: Char,
    val displayLetter: String = baseLetter.toString(),
    val word: String,
    val imageRes: Int
)

object AlphabetAssets {

    val items: List<AlphabetItem> = listOf(
        AlphabetItem('A', "A", "albină", R.drawable.alphabet_a_albina),
        AlphabetItem('B', "B", "balon", R.drawable.alphabet_b_balon),
        AlphabetItem('C', "C", "cal", R.drawable.alphabet_c_cal),
        AlphabetItem('D', "D", "dinozaur", R.drawable.alphabet_d_dinozaur),
        AlphabetItem('E', "E", "elefant", R.drawable.alphabet_e_elefant),
        AlphabetItem('F', "F", "floare", R.drawable.alphabet_f_floare),
        AlphabetItem('G', "G", "girafă", R.drawable.alphabet_g_girafa),
        AlphabetItem('H', "H", "hipopotam", R.drawable.alphabet_h_hipopotam),
        
        // I - Iepure
        AlphabetItem('I', "I", "iepure", R.drawable.alphabet_i_iepure),
        
        // Î - Înghețată (fișierul este alphabet_i_inghetata)
        AlphabetItem('Î', "Î", "înghețată", R.drawable.alphabet_i_inghetata),
        
        AlphabetItem('J', "J", "jucărie", R.drawable.alphabet_j_jucarie),
        AlphabetItem('K', "K", "koala", R.drawable.alphabet_k_koala),
        AlphabetItem('L', "L", "leu", R.drawable.alphabet_l_leu),
        AlphabetItem('M', "M", "mașină", R.drawable.alphabet_m_masina),
        AlphabetItem('N', "N", "nor", R.drawable.alphabet_n_nor),
        AlphabetItem('O', "O", "oaie", R.drawable.alphabet_o_oaie),
        AlphabetItem('P', "P", "pisică", R.drawable.alphabet_p_pisica),
        AlphabetItem('Q', "Q", "quokka", R.drawable.alphabet_q_quokka),
        AlphabetItem('R', "R", "rață", R.drawable.alphabet_r_rata),
        AlphabetItem('S', "S", "soare", R.drawable.alphabet_s_soare),
        
        // Ș - Șoarece (fișierul este alphabet_s_soarece)
        AlphabetItem('Ș', "Ș", "șoarece", R.drawable.alphabet_s_soarece),
        
        AlphabetItem('T', "T", "tren", R.drawable.alphabet_t_tren),
        
        // Ț - Țestoasă (fișierul este alphabet_t_testoasa)
        AlphabetItem('Ț', "Ț", "țestoasă", R.drawable.alphabet_t_testoasa),
        
        AlphabetItem('U', "U", "urs", R.drawable.alphabet_u_urs),
        AlphabetItem('V', "V", "veveriță", R.drawable.alphabet_v_veverita),
        AlphabetItem('X', "X", "xilofon", R.drawable.alphabet_x_xilofon),
        AlphabetItem('Y', "Y", "yoyo", R.drawable.alphabet_y_yoyo),
        AlphabetItem('Z', "Z", "zebră", R.drawable.alphabet_z_zebra)
    )

    // Funcție pentru a asocia literele cu diacritice cu cele de bază (pentru logică)
    fun normalizeBase(token: String): Char {
        if (token.isEmpty()) return ' '
        val c = token[0]
        return when (c) {
            'Ș', 'ș' -> 'S'
            'Ț', 'ț' -> 'T'
            'Î', 'î', 'Â', 'â' -> 'I'
            'Ă', 'ă' -> 'A'
            else -> c.uppercaseChar()
        }
    }

    fun findByDisplay(display: String): AlphabetItem? =
        items.firstOrNull { it.displayLetter.equals(display, ignoreCase = true) }
}

// Resursele UI (imagini de fundal, iconițe, efecte) - Rămân neschimbate
object AlphabetUi {

    object Backgrounds {
        val sky = R.drawable.bg_alphabet_sky
        val city = R.drawable.bg_alphabet_city
        val foreground = R.drawable.bg_alphabet_foreground
        val menu = R.drawable.bg_alphabet_main3
    }

    object Mascot {
        val normal = R.drawable.alphabet_mascot_fox
        val happy = R.drawable.alphabet_mascot_happy
        val surprised = R.drawable.alphabet_mascot_surprised
        val thinking = R.drawable.alphabet_mascot_thinking
    }

    object Icons {
        val correct = R.drawable.icon_alphabet_correct
        val wrong = R.drawable.icon_alphabet_wrong
        val star = R.drawable.icon_alphabet_star
        val home = R.drawable.icon_alphabet_home
        val replay = R.drawable.icon_alphabet_replay
        val soundOn = R.drawable.icon_alphabet_sound_on
    }

    object Effects {
        val confetti = R.drawable.fx_confetti_spritesheet
        val glow = R.drawable.fx_glow_particles
    }

    object Cards {
        val main = R.drawable.alphabet_letter_card_blank
    }
}