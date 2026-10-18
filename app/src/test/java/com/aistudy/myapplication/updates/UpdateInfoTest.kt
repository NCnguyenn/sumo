package com.aistudy.myapplication.updates

import org.junit.Assert.*
import org.junit.Test

class UpdateInfoTest {
    private val installed=InstalledBuild("com.aistudy.myapplication",1,37,setOf("trusted"))
    private val info=UpdateInfo(1,installed.applicationId,2,"test2",36,"https://updates.example.invalid/2.apk",100,"a".repeat(64),"fix","test")
    private val archive=ArchiveBuild(installed.applicationId,2,36,installed.signers)
    @Test fun A01_newMetadataAndActualMatchingArchiveAreAccepted() {
        assertEquals(info,info.validate(installed)); info.verifyArchive(installed,archive,100,"a".repeat(64))
    }
    @Test fun A02_A06_rejectWrongIdentityOldVersionSchemaAndDevice() {
        for(bad in listOf(info.copy(schemaVersion=2),info.copy(applicationId="other"),info.copy(versionCode=1),info.copy(versionCode=0),
            info.copy(minSdk=38),info.copy(sizeBytes=0),info.copy(sizeBytes=101L*1024*1024),info.copy(channel="release"),
            info.copy(sha256="abc"),info.copy(apkUrl="http://example.invalid/2.apk"))) {
            assertThrows(IllegalArgumentException::class.java){bad.validate(installed)}
        }
    }
    @Test fun A04_A05_A06_archiveCannotBorrowMetadataIdentitySignatureOrVersion() {
        for(bad in listOf(archive.copy(applicationId="other"),archive.copy(versionCode=3),archive.copy(minSdk=37),
            archive.copy(signers=setOf("attacker")),archive.copy(signers=emptySet())))
            assertThrows(IllegalArgumentException::class.java){info.verifyArchive(installed,bad,100,"a".repeat(64))}
        assertThrows(IllegalArgumentException::class.java){info.verifyArchive(installed,archive,99,"a".repeat(64))}
        assertThrows(IllegalArgumentException::class.java){info.verifyArchive(installed,archive,100,"b".repeat(64))}
    }
    @Test fun A08_redirectTargetsAlsoRequireHttpsWithoutUserInfoOrFragment() {
        assertEquals("https://example.invalid/file",requireHttps("https://example.invalid/file"))
        for(url in listOf("http://example.invalid","file:///data/local/a.apk","https://user:password@example.invalid","https://example.invalid/#x",""))
            assertThrows(IllegalArgumentException::class.java){requireHttps(url)}
    }
}
