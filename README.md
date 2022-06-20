# PKCE-MeetingSDK
PKCE MeetingSDK Android sample

1. First singin to marketplace.zoom.us and add SDK app from "Build App" (right top corner).
2. Add an existing web domains for your "Redirect URL for OAuth" and "OAuth allow list".
3. Get your SDK and OAuth credentials 
4. Download the latest Android SDK package and unzip it.
5. Clone this repo.
6. Copy "build.gradle" and "commonlib.aar" from step "4." under commonlib folder.
7. Copy "build.gradle" and "mobilertc.aar" from step "4." under mobilertc folder.
8. Now open the project on your Android Studio.
9. Enter credentials and values which can be retrived from makretplace.zoom.us.
*Note) REDIRECT_URL_ENCODED will be an encoded string of your redirect url.
*Note) You will need to add two scopes on your marketplace entry.
 - User > View your user information
 - User > View user's zak token

At this point tested using...
 - zoom-sdk-android-5.10.6.6361
 - Android Studio Chipmunk | 2021.2.1 Patch 1

