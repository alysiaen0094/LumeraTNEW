package com.lumera.app.data.activation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "activation_prefs"

        private const val KEY_ACTIVATED = "activated"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACTIVATED_AT = "activated_at"

        const val TROY_BASE_URL = "https://wuomtznsotoqkk.masa.st"
        const val TROY_CINEMETA_MANIFEST_URL = "$TROY_BASE_URL/cinemeta/manifest.json"
        const val OPENSUBTITLES_MANIFEST_URL = "https://opensubtitles-v3.strem.io/manifest.json"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun isActivated(): Boolean {
        return prefs.getBoolean(KEY_ACTIVATED, false) && !getUserId().isNullOrBlank()
    }

    fun markActivated(userId: String) {
        prefs.edit()
            .putBoolean(KEY_ACTIVATED, true)
            .putString(KEY_USER_ID, userId.trim())
            .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun getAuthCode(): String {
        return getUserId().orEmpty()
    }

    fun getUserAddonManifestUrl(): String? {
        val userId = getUserId() ?: return null
        return "$TROY_BASE_URL/$userId/manifest.json"
    }

    fun getDefaultAddonManifestUrls(): List<String> {
        val userAddonUrl = getUserAddonManifestUrl() ?: return emptyList()

        return listOf(
            TROY_CINEMETA_MANIFEST_URL,
            OPENSUBTITLES_MANIFEST_URL,
            userAddonUrl,
            "https://btttr.cc/RYxBCoAgFETvMmu7gFeJFh8VCTRFf4sQ7x5q5u7NzGMKFDG5YDPkDkdsMm8eYmKGAAdNTy8HrY4goII_L9vnD_PCJnAyl57KH5oUQ7wdpT5MHucRh4A3TJCl1hc/manifest.json",
            "https://aiolists.elfhosted.com/H4sIAAAAAAAAA81W23LiOBD9lSk9AzG3MPEbt3AJxgGbgJ1KTQm7sQW-jSQD9lT-fUu2SUg22cnWTnbmUaePWurTrVb_QDgiN5AgGaESopG9bp-tuW-vO4ApUD3cQYBkBMnYXQ8sopLxaJGOqlMyvqoI0F7OBOiag8Vx6s8a6nJRV_xRXdGN5rTnbQ197qt6_2DUTNfYdlxVG7GR3yQmGV0quiVNe06q9mbJVGs0FX13UHrt46Q7Tu3liKhkVFN77YOyNI6q7qRGanpT3agbqb1T9bvddNAngmvUrg7mSiGqx4ix6kSr-tgzljOy0g7E9u8Sq-bt1-K8lVQ5Xo_LumLtff1yshmtpMG6FzRX192t972mpq451DfhBgbXmrsohNCAMRIGI_tZm7ZlhXHAz6EFAxpgH3LEB45tzLEWxtQSmEUCEGDBnuDAibFTsDnFO962LGDspPcJncOGAnNfw_1jRCiwNkdyEHtegb68g0cYV6kNFMn3yAp9Pw4ITypr4BxoFDIOlH0Tlyl7mAPj3_xwTwCVPsItMzc8sG8MKAH2sy08tHHyMe8Z9d87L-OA-PCBHUKlchRGsYfpz2_0gl04fyghDzO-iGzMQeS_JtUuy1KrXG3p1a9yrSZLjUpDkswiA0wpSgHJPx5LyCW2DcFEGJB8_1BCFPxwD_YZYsWMh74AptgHlu_zgTpPLAHkLAVsgvUkgjMq8aOQcrDbth0GAnsvQGEiIoT37CVUVFMng2-f4D1Q8SSQjKoVqSKJUEMnRDJyOY-YfHGx5pzTimVdCLwSBQ4qiYbTwQwW1HuLODcOxzVTDr3-av_V31TbNGrvbq4nu6Q-mvUIM3bt5dAcd40OKUMyaGG6GR4liAeKE2nppD9IR02_fqV0O9K0rTVul4HZlCbl7ZxRTzFNqTOshql2TFOst0J66w633WO_fHM5XdZVQrddc2H070x7-T2Ze4qkrxJ6tadNvXy84dowkJrVnu01Dp1Wst_352tvqK-S-Ta5q84afuuyD2Qbbrvr1Bw6aouS6swedIaBXkvXvcOFKDHMsRc6IsP_rPmbDzKkxCEB9rJ-c0Y4Myk4IBtgeUt6P58nuigZJKPTAUWaJ5nXL4pABZu_ZMGRU6zFUV5eWSjFRrYjESohwubwPSZUGDfYY_D4UOx6xu8fHksfl-B1K3hPiZz3n_V4OuelINoJ5q9o_6siL9vo35XI7L-6JPQw-lJr_hkl8ebn8I4On1QQhRx_UEG8_vre0yOjfZYe7cL5b5PjzX_9pRTnlF_9Sm5zt7__mbw1sXyqEK8L4qTEp7-Qh9y1-FGfZH4e0SiwbPLOzMXnW4zlwkxYXrH5ASXkYlakTuY0zgEtayDZ-vGxhFhI-S2FDVAIrNOoZROG1x4MIKBwTTwuZu3cAQTCMMeBnY9y14B5TJ9PpJlF6XWE8TS3Z7cVjYsTHxrVS1RCDqaJRfFhE1LbsbKZMsAsiikJiJDTtbMhUwTFAFPL1Z7ifjhNjdorQ9YWxI6sJ-TWfnZfu4j3LzqhhGAeDgAA/manifest.json",
            "https://aiolists.elfhosted.com/H4sIAAAAAAAAA81X23LaOhT9lY6egdhcQsMbt3AJxgGbgN1hOsIStsC3SjJgd_LvHdkmCTSc5sxJTvrotZe2rKUlae-fAIbkDsegAUAB0BCtmi--uYdWLQwppnqwxT5oABwPnVXPIioZDmbJQB6T4U1JgGg-EaBj9maHsTepqvNZRfEGFUU3auOOuzH0qafq3b1RNh1j03JUbcAGXo2YZHCt6JY07tiJ2pnEY61aU_TtXuk0D6P2MEHzAVHJoKx2mntlbhxU3U6MxHTHulExErRV9YftuNclgmuUb_bmQiGqy4ixaIWLytA15hOy0PYEeQ-xVXZ3KzHfQiodbodFXbF2nn49Wg8WUm_V8WuL2_bG_VFWE8fs6-tgjXu3mjPLhdAwYyTwB-hZm6ZlBZHPX0IzhqkPPZwhHuYQQQ61IKKWwAQlZ46gb0fQzpmcwi1vWhZm7Kj1EZ3iNcXMOYe7h5BQzJocNPzIdXP0dH6XMK5ShClofANW4HmRT3hcWmHOMQ0DxjFl38XPFF3IMePfvWBHMCi8hVtkTrBn3xmmBLM_DeEBgvHbsqfUf5-8CH3i4TeMECoVOcU-Ir795186pb8xfRiEkQvpG7Mf2XnyZQG4kPFZiCDHwlplqXxdlOpFua7LXxtytSFLJUkum_kGMyV3GWj8fCwAhyCE_ZEIgMa3ZQFQ7AU7jF4gVsR44AlgDD3MsnEepvYTSwAZS8GIQD0O8Qsq8cKAcoyaCAW-wC4tUISIWMKleAHkZm2l8P0TvMNUnDbQAHJJKkliqYEdgAZwOA9Z4-pqxTmnJcu6Engp9G1QEHdZCzI8o-5rxKmxP6yYsu90F7uv3lpu0rC5vbsdbePKYNIhzNg2531z2DZapIjjXh3Sdf8g4ain2KGWjLq9ZFDzKjdKuyWNm1r1fu6bNWlU3EwZdRXTlFp9OUi0Q5JAvR7Qe6e_aR-6xbvr8byiErppmzOj-2Ci-Y946iqSvojpzY7W9OLhjmt9X6rJHeRW9616vNt1pyu3ry_i6SZ-kCdVr37dxWQTbNqrxOzbap0SeYJ6rb6vl5NVZ38lLAY5dANb7PA_a_7qeQ8osYkP3fQqe0F4EVKgT9aYZbfd5f080oVlQAMcJ8i3eZRm_aIIVLD5KQsfOIVaFGb2SpeSD2RbEoICIGyKf0SEiuAaugw_LvNRz_i35WPh7RKc3zSXlMh4_1mPp3lOBdGOMD-j_a-KnN7SvyuRxt_bEnoQfinX_g5LvPr2XNDhgwyRy_EXGeL8Zb2kR0r7KD2aefJPk-P1suFUixPOu5-TPO_nn5QLJdFHavGbK45ifP45ebXge0WLnPLetrjP0v4lrjgrZT9UiHNPHJX4cEsss9Si1HqS-bl2p5il3V4azquyvBUUYcKyqyyboAAcyPKta3AaZYCWvizp9-NjAbCA8nuK15hi3zrW4IgwuHJxD_sU3xKXix4vS4B9EZhCH2U1_i2GPKLPM9I0onRaInjsF9O_FS8aJx6uytegAGxIY4vC_TqgyLbSZsOHLIwo8YmQ00Fp9yEWxTCklqM9rXt5bCe0s0DWAC8LIH0ssmg3_V-Ur_cXzZchsZIQAAA/manifest.json"
        )
    }

    fun clearActivation() {
        prefs.edit().clear().apply()
    }
}
