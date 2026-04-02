package hoang.dqm.codebase.data

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class AppInfo(
    @SerializedName("id")
    val appId: String,
    @SerializedName("app_name")
    val appName: String,
) : Parcelable {
}