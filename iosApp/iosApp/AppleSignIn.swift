import AuthenticationServices
import ComposeApp
import CryptoKit
import UIKit

/// Implements the Kotlin `AppleSignInBridge` with AuthenticationServices. Registered at startup
/// through `AppleSignInBridgeHolder`. Each request carries a fresh nonce: Apple puts its SHA-256
/// in the ID token and our server checks it against the raw value, so a token can't be replayed.
final class AppleSignIn: NSObject, AppleSignInBridge {
    private var pending: (callback: SignInCallback, rawNonce: String)?
    private var controller: ASAuthorizationController?

    func signIn(callback: SignInCallback) {
        DispatchQueue.main.async { self.start(callback) }
    }

    func makeButton(dark: Bool, onTap: @escaping () -> Void) -> UIView {
        let button = ASAuthorizationAppleIDButton(type: .signIn, style: dark ? .white : .black)
        button.addAction(UIAction { _ in onTap() }, for: .touchUpInside)
        return button
    }

    private func start(_ callback: SignInCallback) {
        guard pending == nil else {
            callback.onFailure(cancelled: false, message: "Sign-in already in progress")
            return
        }
        let rawNonce = Self.randomNonce()
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email]
        request.nonce = Self.sha256Hex(rawNonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        pending = (callback, rawNonce)
        self.controller = controller
        controller.performRequests()
    }

    private func finish() -> (callback: SignInCallback, rawNonce: String)? {
        defer {
            pending = nil
            controller = nil
        }
        return pending
    }

    private static func randomNonce() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else {
            fatalError("No secure random bytes")
        }
        return Data(bytes).base64EncodedString()
    }

    private static func sha256Hex(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}

extension AppleSignIn: ASAuthorizationControllerDelegate {
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let pending = finish() else { return }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8)
        else {
            pending.callback.onFailure(cancelled: false, message: "Apple returned no identity token")
            return
        }
        pending.callback.onSuccess(idToken: idToken, rawNonce: pending.rawNonce)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        guard let pending = finish() else { return }
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        pending.callback.onFailure(cancelled: cancelled, message: error.localizedDescription)
    }
}

extension AppleSignIn: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first ?? ASPresentationAnchor()
    }
}
