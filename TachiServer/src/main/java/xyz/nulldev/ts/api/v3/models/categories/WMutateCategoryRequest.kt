package xyz.nulldev.ts.api.v3.models.categories

data class WMutateCategoryRequest(
        override val name: String?,
        override val order: Int?
) : WMutateCategoryRequestBase