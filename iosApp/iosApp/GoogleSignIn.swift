import AuthenticationServices
import ComposeApp
import CryptoKit
import UIKit

/// Implements the Kotlin `GoogleSignInBridge` without Google's SDK: the standard OAuth flow for
/// native apps (authorization code + PKCE, RFC 8252) in an `ASWebAuthenticationSession`, then the
/// code is exchanged for tokens and the **ID token** goes to our server, which verifies it like an
/// Android Google token. Needs the iOS OAuth client id in `GOOGLE_IOS_CLIENT_ID`
/// (Configuration/Config.xcconfig → Info.plist `LiftlyGoogleClientID`); without it Google is hidden.
final class GoogleSignIn: NSObject, GoogleSignInBridge {
    private let clientId: String?
    private var session: ASWebAuthenticationSession?

    override init() {
        let value = Bundle.main.object(forInfoDictionaryKey: "LiftlyGoogleClientID") as? String
        clientId = value.flatMap { $0.hasSuffix(".apps.googleusercontent.com") ? $0 : nil }
        super.init()
    }

    var isConfigured: Bool { clientId != nil }

    func signIn(callback: SignInCallback) {
        DispatchQueue.main.async { self.start(callback) }
    }

    private func start(_ callback: SignInCallback) {
        guard let clientId, session == nil else {
            callback.onFailure(cancelled: false, message: clientId == nil ? "Google sign-in is not configured" : "Sign-in already in progress")
            return
        }
        // Google's iOS clients redirect to the reversed client id scheme.
        let scheme = "com.googleusercontent.apps." + clientId.replacingOccurrences(of: ".apps.googleusercontent.com", with: "")
        let redirectUri = scheme + ":/oauthredirect"
        let verifier = Self.random()
        let state = Self.random()
        var auth = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        auth.queryItems = [
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "redirect_uri", value: redirectUri),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: "openid email"),
            URLQueryItem(name: "code_challenge", value: Self.base64Url(Data(SHA256.hash(data: Data(verifier.utf8))))),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "state", value: state),
        ]
        let session = ASWebAuthenticationSession(url: auth.url!, callbackURLScheme: scheme) { [weak self] url, error in
            self?.session = nil
            if let error {
                let cancelled = (error as? ASWebAuthenticationSessionError)?.code == .canceledLogin
                callback.onFailure(cancelled: cancelled, message: error.localizedDescription)
                return
            }
            let items = url.flatMap { URLComponents(url: $0, resolvingAgainstBaseURL: false)?.queryItems } ?? []
            guard items.first(where: { $0.name == "state" })?.value == state,
                  let code = items.first(where: { $0.name == "code" })?.value
            else {
                // "error=access_denied" when the user declines on Google's page.
                let denied = items.contains { $0.name == "error" }
                callback.onFailure(cancelled: denied, message: "Google returned no authorization code")
                return
            }
            Self.exchange(code: code, verifier: verifier, clientId: clientId, redirectUri: redirectUri, callback: callback)
        }
        session.presentationContextProvider = self
        session.prefersEphemeralWebBrowserSession = false // reuse the Google session from Safari
        self.session = session
        session.start()
    }

    /// Code → tokens. iOS clients have no secret; PKCE proves this app started the flow.
    private static func exchange(code: String, verifier: String, clientId: String, redirectUri: String, callback: SignInCallback) {
        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        var form = URLComponents()
        form.queryItems = [
            URLQueryItem(name: "code", value: code),
            URLQueryItem(name: "client_id", value: clientId),
            URLQueryItem(name: "redirect_uri", value: redirectUri),
            URLQueryItem(name: "grant_type", value: "authorization_code"),
            URLQueryItem(name: "code_verifier", value: verifier),
        ]
        request.httpBody = form.percentEncodedQuery?.data(using: .utf8)
        URLSession.shared.dataTask(with: request) { data, _, error in
            let json = data.flatMap { try? JSONSerialization.jsonObject(with: $0) as? [String: Any] }
            DispatchQueue.main.async {
                if let idToken = json?["id_token"] as? String {
                    callback.onSuccess(idToken: idToken, rawNonce: nil)
                } else {
                    callback.onFailure(cancelled: false, message: error?.localizedDescription ?? "Google returned no ID token")
                }
            }
        }.resume()
    }

    private static func random() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
            fatalError("No secure random bytes")
        }
        return base64Url(Data(bytes))
    }

    private static func base64Url(_ data: Data) -> String {
        data.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

extension GoogleSignIn: ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first ?? ASPresentationAnchor()
    }
}
