package com.sbros.app.domain.model

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE
}

enum class AppSyncState {
    IDLE,
    SYNCING,
    OFFLINE,
    ERROR,
    NOT_AUTHENTICATED
}
