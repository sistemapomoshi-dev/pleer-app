package com.hiresplayer.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import kotlin.coroutines.Continuation;
import org.junit.Test;

public class CloudProviderContractTest {
    @Test
    public void providerExposesStableIdentity() {
        FakeCloudProvider provider = new FakeCloudProvider();

        assertEquals("fake", provider.getId());
        assertEquals("Fake Cloud", provider.getTitle());
    }

    @Test
    public void providerUsesSharedCloudFileModel() {
        List<CloudFile> files = FakeCloudProvider.sampleFiles();

        assertTrue(files.stream().anyMatch(file -> file.getType() == CloudFileType.Directory));
        assertTrue(files.stream().anyMatch(file -> file.getType() == CloudFileType.File));
        assertEquals("track.flac", files.get(1).getName());
    }

    private static class FakeCloudProvider implements CloudProvider {
        @Override
        public String getId() {
            return "fake";
        }

        @Override
        public String getTitle() {
            return "Fake Cloud";
        }

        @Override
        public Object list(String path, Continuation<? super List<CloudFile>> continuation) {
            return sampleFiles();
        }

        @Override
        public Object getDownloadUrl(String path, Continuation<? super String> continuation) {
            return "https://example.test/download/" + path.substring(path.lastIndexOf("/") + 1);
        }

        @Override
        public Object signOut(Continuation<? super kotlin.Unit> continuation) {
            return kotlin.Unit.INSTANCE;
        }

        static List<CloudFile> sampleFiles() {
            return Arrays.asList(
                new CloudFile("disk:/Music", "Music", "disk:/Music", CloudFileType.Directory, null, null, null),
                new CloudFile("disk:/track.flac", "track.flac", "disk:/track.flac", CloudFileType.File, 42_000_000L, "audio/flac", null)
            );
        }
    }
}
