import ComposeApp
import Foundation
import Security

/// Implements the Kotlin `SecureStore` (session tokens) with the Keychain: generic passwords for
/// this app only, readable after the first unlock (so background sync can use them), and never
/// migrated to another device through backups.
final class KeychainStore: NSObject, SecureStore {
    private let service = "com.himanshu.liftly.session"

    func get(key: String) -> String? {
        var query = baseQuery(key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: AnyObject?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func put(key: String, value: String) {
        let data = Data(value.utf8)
        let update = [kSecValueData as String: data]
        if SecItemUpdate(baseQuery(key) as CFDictionary, update as CFDictionary) == errSecItemNotFound {
            var add = baseQuery(key)
            add[kSecValueData as String] = data
            add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(add as CFDictionary, nil)
        }
    }

    func remove(key: String) {
        SecItemDelete(baseQuery(key) as CFDictionary)
    }

    private func baseQuery(_ key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key,
        ]
    }
}
