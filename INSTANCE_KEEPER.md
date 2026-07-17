# 🎓 Cours — InstanceKeeper en pratique : migrer ton `RouterViewModel` vers `DetectionComponent`

> **Contexte du cours** : tu as déjà suivi le cours général sur Decompose, et tu es en train de migrer une vraie fonctionnalité — un scanner/connecteur de routeurs Mikrotik — d'un `ViewModel` Android classique vers un `DetectionComponent` Decompose. J'ai lu ton code (`RouterViewModel`, `DetectionComponent`, `DefaultDetectionComponent`, `RootComponent`). On va **terminer la migration ensemble**, méthode par méthode, en comprenant à chaque étape *pourquoi* on écrit le code ainsi. On s'appuie sur ton code réel du début à la fin — pas d'exemple jouet cette fois.

> ⚠️ **Avant de commencer** : j'ai repéré **deux problèmes dans ton code actuel** qui empêcheront la compilation ou casseront le comportement une fois branché. On les corrige en premier (section 2.2), avant même de parler d'`InstanceKeeper` — sinon tout ce qu'on construira par-dessus sera instable.

---

## 1. Introduction et vue d'ensemble

### Rappel du problème

Ton `RouterViewModel` utilise `viewModelScope`, un `CoroutineScope` fourni gratuitement par `androidx.lifecycle.ViewModel`, automatiquement annulé quand le ViewModel est détruit. C'est pratique, mais **`viewModelScope` n'existe que sur Android**. Ton `DefaultDetectionComponent` ne peut pas l'utiliser.

Ce que tu as déjà bien fait dans `DefaultDetectionComponent` :
- `MutableValue(RouterUiState())` à la place de `MutableStateFlow` ✅ (le `Value` de Decompose remplace le `StateFlow` exposé à l'UI)
- La délégation `ComponentContext by componentContext` ✅
- Un début d'appel à `instanceKeeper.getOrCreate { }` dans `startScan()` — c'est le bon réflexe, mais l'implémentation est vide pour l'instant. **C'est exactement ce qu'on va compléter.**

### Ce qu'on va construire dans ce cours

| Étape | Ce qu'on fait |
|---|---|
| 1 | Corriger les 2 incohérences actuelles entre `RootComponent` et `DefaultDetectionComponent` |
| 2 | Créer l'`InstanceHolder` qui porte le `CoroutineScope` (remplace `viewModelScope`) |
| 3 | Migrer `startScan()`, `selectRouter()`, `connect()`, `submitLogin()`, `testConnection()` un par un |
| 4 | Nettoyer l'interface publique `DetectionComponent` (ce qui doit rester privé) |
| 5 | Vérifier que la navigation vers l'écran `Login` se déclenche bien après connexion réussie |

---

## 2. Cours détaillé

### 2.1 Analyse comparative : `RouterViewModel` vs `DetectionComponent`

| Dans `RouterViewModel` | Équivalent Decompose | Où dans ton code |
|---|---|---|
| `class RouterViewModel : ViewModel()` | `class DefaultDetectionComponent(componentContext: ComponentContext) : DetectionComponent, ComponentContext by componentContext` | déjà fait ✅ |
| `viewModelScope` | Un `CoroutineScope` porté par un `InstanceKeeper.Instance` | à créer (section 2.3) |
| `_state = MutableStateFlow(...)` / `val state = _state.asStateFlow()` | `_state = MutableValue(...)` / `override val state: Value<...> = _state` | déjà fait ✅ |
| `init { ... }` dans le ViewModel | `init { ... }` dans le Default component (le `ComponentContext` est déjà disponible à ce stade) | déjà fait ✅ |
| `viewModelScope.launch { ... }` | `scope.launch { ... }` où `scope` vient de l'InstanceHolder | à faire (section 2.4) |
| Détruit automatiquement par le système Android (`onCleared()`) | Détruit quand le composant sort **définitivement** de la pile Decompose (`InstanceKeeper.Instance.onDestroy()`) | à faire (section 2.3) |

> 🔑 Le point important à retenir : **la logique métier ne change quasiment pas**. Ce qui change, c'est uniquement *où vit l'état* (`MutableValue` au lieu de `MutableStateFlow`) et *qui porte le scope de coroutines* (`InstanceKeeper` au lieu du système Android). Tu n'as pas besoin de réécrire tes algorithmes de scan ou de connexion Mikrotik.

---

### 2.2 Corriger les fondations avant de migrer

#### Problème n°1 — Le nom de classe ne correspond pas

Dans `RootComponent.kt`, tu écris :
```kotlin
is Config.Detection -> RootComponent.Child.Detection(
    DefaultDetecteurComponent(   // <-- "Detecteur"
        componentContext = componentContext,
        onConnectedClicked = { ipAddress -> navigation.push(Config.Login(ipAddress)) },
    )
)
```
Mais ton fichier s'appelle `DefaultDetectionComponent.kt` et déclare `class DefaultDetectionComponent(...)` — **pas** `DefaultDetecteurComponent`. Ce sont deux noms différents (un vestige probable d'un renommage partiel). **Ça ne compile pas en l'état.**

**Correction** — dans `RootComponent.kt` :
```kotlin
is Config.Detection -> RootComponent.Child.Detection(
    DefaultDetectionComponent(   // nom corrigé
        componentContext = componentContext,
        onConnectClicked = { ipAddress -> navigation.push(Config.Login(ipAddress)) },
    )
)
```

#### Problème n°2 — La signature du callback ne correspond pas

`RootComponent` appelle le paramètre `onConnectedClicked` avec **un argument** (`ipAddress`) :
```kotlin
onConnectedClicked = { ipAddress -> navigation.push(Config.Login(ipAddress)) }
```
Mais `DefaultDetectionComponent` déclare :
```kotlin
private val onConnectClicked: () -> Unit  // aucun paramètre !
```
Un lambda `(String) -> Unit` ne peut pas être assigné à un paramètre de type `() -> Unit` — encore une erreur de compilation.

**Correction** — dans `DefaultDetectionComponent.kt` :
```kotlin
class DefaultDetectionComponent(
    componentContext: ComponentContext,
    private val onConnectClicked: (ipAddress: String) -> Unit, // reçoit l'IP du routeur connecté
) : DetectionComponent, ComponentContext by componentContext {
```

> 💡 **Pourquoi c'est important pédagogiquement** : ce genre d'incohérence entre le parent (qui *fournit* le callback) et l'enfant (qui le *déclare*) est **l'erreur la plus fréquente** en Decompose dès qu'on a plusieurs allers-retours dans le code. Le compilateur te protège ici — si ça ne compile pas à cause d'un callback, vérifie **toujours en premier** que la signature déclarée dans l'enfant correspond exactement à ce que le parent lui fournit dans `createChild`.

---

### 2.3 L'`InstanceHolder` : porter le `CoroutineScope` avec `InstanceKeeper`

C'est la pièce manquante centrale. On crée une petite classe interne dont le **seul rôle** est de porter un `CoroutineScope` et de l'annuler proprement à la destruction :

```kotlin
private class InstanceHolder : InstanceKeeper.Instance {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onDestroy() {
        scope.cancel()
    }
}
```

Puis, en haut de `DefaultDetectionComponent`, on l'instancie **une seule fois** via `instanceKeeper.getOrCreate` et on expose un accesseur pratique :

```kotlin
class DefaultDetectionComponent(
    componentContext: ComponentContext,
    private val onConnectClicked: (ipAddress: String) -> Unit,
) : DetectionComponent, ComponentContext by componentContext {

    private val instanceHolder = instanceKeeper.getOrCreate { InstanceHolder() }
    private val scope get() = instanceHolder.scope

    // ...
}
```

> 🔑 **Pourquoi `getOrCreate` et pas juste `= InstanceHolder()` directement ?** Parce que `DefaultDetectionComponent` peut être recomposé/reconstruit (ex: recomposition Compose) sans que le composant **logique** ne quitte la pile de navigation. `getOrCreate` garantit qu'on **réutilise le même scope** tant que le composant reste dans la pile, et qu'on n'en crée un nouveau que la toute première fois. C'est le remplaçant direct de `viewModelScope`, qui avait exactement cette même garantie côté Android.

> ⚠️ **Piège à éviter** : ne mets **jamais** `val scope = CoroutineScope(...)` directement dans le corps de la classe sans passer par `instanceKeeper` — ce scope serait recréé à chaque fois que Decompose reconstruit l'instance (ce qui peut arriver plus souvent qu'on ne le pense), annulant silencieusement les coroutines en cours à chaque fois. C'est un bug très difficile à diagnostiquer car il ne plante pas — il "oublie" juste des opérations en cours.

> 🧪 **Teste maintenant** : ajoute temporairement un `println("InstanceHolder créé")` dans le `init` implicite d'`InstanceHolder`, lance l'app, force une recomposition (ex: change la taille de la fenêtre sur Desktop). Le message ne doit s'afficher **qu'une seule fois**, pas à chaque recomposition — c'est la preuve que `getOrCreate` fonctionne comme prévu.

---

### 2.4 Migrer la logique, méthode par méthode

#### `startScan()`

Ton code actuel :
```kotlin
override fun startScan() {
    instanceKeeper.getOrCreate {
    }
}
```

Version migrée (logique identique au ViewModel, juste `scope.launch` au lieu de `viewModelScope.launch`) :
```kotlin
override fun startScan() {
    if (_state.value.isScanning) return

    scope.launch {
        _state.value = _state.value.copy(
            isScanning = true,
            progress = 0,
            routers = emptyList()
        )

        for (baseIp in listeBaseIp) {
            _state.value = _state.value.copy(baseIp = baseIp)

            val results = scanner.scanWifi(baseIp) { progress ->
                _state.value = _state.value.copy(progress = progress)
            }

            _state.value = _state.value.copy(
                isScanning = false,
                routers = results,
                progress = 254
            )

            if (results.isNotEmpty()) break
        }
    }
}
```

> 🔑 Remarque : on a déplacé la vérification `if (_state.value.isScanning) return` **hors** du `launch`, directement au début de la fonction — comme dans ton ViewModel original (`if (_state.value.isScanning) return@launch`). Les deux fonctionnent, mais sortir tôt avant même de lancer une coroutine évite de payer le coût d'une coroutine inutile.

#### `selectRouter()`

Pas besoin de coroutine ici — c'est une simple mise à jour d'état synchrone, exactement comme dans le ViewModel :
```kotlin
override fun selectRouter(routeur: Routeur) {
    if (_state.value.selectedRouter != routeur) {
        _state.value = _state.value.copy(selectedRouter = routeur)
    } else {
        _state.value = _state.value.copy(selectedRouter = null)
    }
}
```

#### `connect()` et `dismissLoginCard()`

Également synchrones — pas de `scope.launch` nécessaire :
```kotlin
override fun connect() {
    val router = _state.value.selectedRouter ?: return
    val saved = credentialsStore.getCredentials()

    if (saved != null) {
        testConnection(router.ipAddress, saved.first, saved.second)
    } else {
        _state.value = _state.value.copy(showLoginCard = true, loginError = null)
    }
}

override fun dismissLoginCard() {
    _state.value = _state.value.copy(showLoginCard = false, loginError = null)
}
```

#### `submitLogin()` — ici on a besoin du scope, et on déclenche la navigation

```kotlin
override fun submitLogin(username: String, password: String) {
    val router = _state.value.selectedRouter ?: return

    if (username.isBlank() || password.isBlank()) {
        _state.value = _state.value.copy(
            loginError = "Veuillez renseigner l'identifiant et le mot de passe"
        )
        return
    }

    scope.launch(Dispatchers.IO) {
        _state.value = _state.value.copy(isLoggingIn = true, loginError = null)

        val client = MikrotikRawClient(router.ipAddress)

        val portOk = client.testPort()
        if (!portOk) {
            _state.value = _state.value.copy(isLoggingIn = false, loginError = "Routeur injoignable")
            return@launch
        }

        val loginOk = client.login(username, password)
        if (!loginOk) {
            client.close()
            _state.value = _state.value.copy(
                isLoggingIn = false,
                loginError = "Identifiant ou mot de passe incorrect"
            )
            return@launch
        }

        credentialsStore.saveCredentials(username, password)

        val result = client.execute("/system/resource/print")
        client.close()

        _state.value = _state.value.copy(
            isLoggingIn = false,
            showLoginCard = false,
            isAuthenticated = true,
            result = result
        )

        onConnectClicked(router.ipAddress) // <-- déclenche la navigation vers l'écran Login (voir RootComponent)
    }
}
```

> 🔑 **Nouveauté par rapport au ViewModel** : ton `RouterViewModel` original ne naviguait nulle part — il se contentait de changer `isAuthenticated`. Ici, `DefaultDetectionComponent` **doit en plus appeler `onConnectClicked(router.ipAddress)`** une fois la connexion réussie, puisque c'est ce callback qui déclenche `navigation.push(Config.Login(ipAddress))` dans `RootComponent`. C'est la différence fondamentale entre un ViewModel (qui ne fait *que* de l'état) et un composant Decompose (qui fait de l'état **et** peut déclencher de la navigation via les callbacks qu'on lui a donnés).

#### `testConnection()` — attention à l'interface publique (voir 2.5)

```kotlin
private fun testConnection(ip: String, username: String, password: String) {
    scope.launch(Dispatchers.IO) {
        val client = MikrotikRawClient(ip)

        val ok = client.testPort()
        if (!ok) return@launch

        val loginOk = client.login(username, password)

        if (!loginOk) {
            client.close()
            credentialsStore.clearCredentials()
            _state.value = _state.value.copy(
                isAuthenticated = false,
                showLoginCard = true,
                loginError = "Session expirée, veuillez vous reconnecter"
            )
            return@launch
        }

        val result = client.execute("/system/resource/print")
        client.close()

        _state.value = _state.value.copy(result = result)

        onConnectClicked(ip)
    }
}
```

---

### 2.5 Nettoyer l'interface publique : ce qui doit rester privé

Regarde ton `DetectionComponent.kt` actuel :
```kotlin
interface DetectionComponent {
    val state : Value<RouterUiState>
    val listeBaseIp : List<String>

    fun startScan()
    fun selectRouter(routeur: Routeur)
    fun connect()
    fun dismissLoginCard()
    fun submitLogin(username: String, password: String)
    fun testConnection(ip: String, username: String, password: String) // <-- problème
}
```

`testConnection(ip, username, password)` est exposée dans l'**interface publique**, donc potentiellement appelable directement depuis `DetectionContent.kt` (la couche Compose). Or, dans ton `RouterViewModel` original, `testConnection` était `private` — elle n'était censée être appelée que **depuis `connect()`**, jamais directement par l'UI.

**Correction recommandée** — retire `testConnection` de l'interface, garde-la `private` dans `DefaultDetectionComponent` (comme fait dans le code de la section 2.4) :

```kotlin
interface DetectionComponent {
    val state: Value<RouterUiState>
    val listeBaseIp: List<String>

    fun startScan()
    fun selectRouter(routeur: Routeur)
    fun connect()
    fun dismissLoginCard()
    fun submitLogin(username: String, password: String)
    // testConnection retirée : détail d'implémentation interne
}
```

> 🔑 **Principe général à retenir** : l'interface `XxxComponent` doit exposer **uniquement** ce dont l'écran Compose a réellement besoin (les actions déclenchables par l'utilisateur). Toute méthode qui n'est qu'un détail d'implémentation interne (appelée uniquement par une autre méthode du même composant) doit rester `private` dans le `Default...`. Ça réduit la surface d'API que l'UI peut mal utiliser, et ça rend l'interface plus facile à comprendre d'un coup d'œil.

---

### 2.6 Bonnes pratiques et pièges spécifiques à cette migration

| Bonne pratique | Pourquoi, dans ton cas précis |
|---|---|
| Toujours vérifier que les callbacks (`onConnectClicked`, etc.) ont la **même signature** entre `RootComponent.createChild` et le constructeur du composant enfant | Cause n°1 d'erreurs de compilation lors d'une migration ViewModel → Decompose |
| Garder `scope` comme `private val ... get() = instanceHolder.scope`, jamais recréé manuellement | Remplace `viewModelScope` de façon fiable, survit aux recompositions |
| Ne remonter dans l'interface publique que les méthodes destinées à l'UI | Évite qu'un détail interne comme `testConnection` soit appelable n'importe où |
| Fermer explicitement `client.close()` dans **chaque** branche (succès et échec) avant `return@launch` | Ton code original le fait déjà bien — vérifie que ça reste vrai après migration, un client Mikrotik oublié ouvert est une fuite de ressource réseau |
| Le callback de navigation (`onConnectClicked`) doit être appelé **après** la mise à jour de `_state`, jamais avant | Garantit que l'état affiché est cohérent au moment où l'écran suivant (Login) se construit |
| `CredentialsStore()` recréé à chaque instanciation du composant — vérifie qu'il ne recharge pas des identifiants obsolètes s'il garde un état interne | Piège potentiel si `CredentialsStore` a lui-même un cache interne non synchronisé avec le stockage réel |

---

## 3. Résumé des points clés

| Élément du ViewModel | Élément Decompose équivalent dans ton code |
|---|---|
| `viewModelScope` | `instanceKeeper.getOrCreate { InstanceHolder() }.scope` |
| `MutableStateFlow` / `.asStateFlow()` | `MutableValue` / exposé comme `Value` |
| `viewModelScope.launch { }` | `scope.launch { }` |
| Détruit par le système Android | Détruit par `InstanceKeeper.Instance.onDestroy()` quand le composant quitte la pile |
| Pas de navigation dans un ViewModel classique | Navigation déclenchée via un callback fourni par le parent (`onConnectClicked`) |
| Toute méthode publique du ViewModel | Seules les méthodes **destinées à l'UI** doivent être dans l'interface `XxxComponent` |
| — | Vérifier systématiquement la cohérence des signatures de callback entre parent et enfant |

---

## 4. Devoirs pratiques

### 📝 Exercice 1 — Ajouter `logout()` à l'interface
Ton `RouterViewModel` avait une méthode `logout()` qui n'existe pas encore dans `DetectionComponent`/`DefaultDetectionComponent`. Ajoute-la en respectant le même principe : elle doit appeler `credentialsStore.clearCredentials()` et mettre à jour `_state` avec `isAuthenticated = false`. Réfléchis : a-t-elle besoin de `scope.launch { }` ou peut-elle rester synchrone ?
*Objectif : vérifier que tu sais identifier toi-même si une méthode a besoin du scope de coroutines ou non.*

### 🔬 Exercice 2 — TP guidé : nettoyer les ressources à la destruction
Actuellement, si l'utilisateur quitte l'écran `Detection` (navigue ailleurs) pendant qu'un scan ou une connexion Mikrotik est en cours, la coroutine est annulée par `InstanceKeeper` (bien), mais si un `MikrotikRawClient` est resté ouvert au moment de l'annulation, sa connexion réseau pourrait ne jamais être fermée proprement. Modifie `InstanceHolder` pour garder une référence au client actif et le fermer explicitement dans `onDestroy()` :
```kotlin
private class InstanceHolder : InstanceKeeper.Instance {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var activeClient: MikrotikRawClient? = null

    override fun onDestroy() {
        activeClient?.close()
        scope.cancel()
    }
}
```
Adapte `submitLogin()` et `testConnection()` pour renseigner `instanceHolder.activeClient` dès la création du `MikrotikRawClient`, et le remettre à `null` après un `client.close()` normal.
*Objectif : comprendre qu'`InstanceKeeper.onDestroy()` est l'endroit correct pour libérer des ressources externes, pas seulement annuler des coroutines.*

### 🧠 Exercice 3 — Question de réflexion + mini-TP de test
1. **Réflexion (5-10 lignes)** : dans `RootComponent`, `Config.Login` transporte `routeurIpAddress: String`. Pourquoi est-ce une bonne pratique de ne transporter que l'adresse IP plutôt que, par exemple, tout l'objet `Routeur` sélectionné ou même les identifiants de connexion ? (relie ta réponse à ce qui a été vu sur le `Config` dans le cours général Decompose).
2. **TP** : écris un test simple (`commonTest`) qui vérifie que `selectRouter()` bascule bien `selectedRouter` entre une valeur et `null` quand on l'appelle deux fois de suite avec le même routeur. Tu n'as pas besoin de mocker `WifiRouterScanner` ni `CredentialsStore` pour ce test précis puisque `selectRouter()` ne les utilise pas.
   *Objectif : prouver que la logique de sélection est testable indépendamment du réseau, exactement comme vu dans la doc de référence sur les tests en `commonTest`.*

---

## 5. Barème et corrigé

<details>
<summary>🔓 Cliquer pour dérouler le corrigé et la grille d'évaluation</summary>

### Exercice 1 — Corrigé attendu
```kotlin
// DetectionComponent.kt (ajout à l'interface)
fun logout()
```
```kotlin
// DefaultDetectionComponent.kt
override fun logout() {
    credentialsStore.clearCredentials()
    _state.value = _state.value.copy(isAuthenticated = false)
}
```
**Grille (sur 10)** :
- Méthode ajoutée dans l'interface ET l'implémentation (3 pts)
- Reconnaissance correcte qu'elle n'a pas besoin de `scope.launch` (opérations synchrones) (4 pts)
- Mise à jour d'état cohérente avec le reste du composant (3 pts)

### Exercice 2 — Corrigé attendu (extrait clé)
```kotlin
scope.launch(Dispatchers.IO) {
    val client = MikrotikRawClient(router.ipAddress)
    instanceHolder.activeClient = client
    // ... logique de login ...
    client.close()
    instanceHolder.activeClient = null
}
```
**Grille (sur 10)** :
- `activeClient` correctement renseigné à la création du client (3 pts)
- `activeClient` remis à `null` après un `close()` normal (2 pts)
- `onDestroy()` ferme bien `activeClient` s'il est encore actif (3 pts)
- Compréhension démontrée que ça couvre le cas où la coroutine est annulée en plein milieu (2 pts)

### Exercice 3 — Corrigé attendu

**Réflexion** : ne transporter que l'adresse IP (et non l'objet `Routeur` complet ou des identifiants) garde le `Config` **minimal et sérialisable simplement** — un `String` se sérialise trivialement, alors qu'un objet `Routeur` complet imposerait de rendre toute sa structure `@Serializable` et alourdirait chaque sauvegarde d'état. Transporter des identifiants de connexion serait en plus un **risque de sécurité** : le `Config` peut être persisté (process death) donc potentiellement écrit sur disque — y mettre un mot de passe en clair serait une mauvaise pratique. L'écran `Login` peut retrouver tout le reste (objet `Routeur`, credentials) via `credentialsStore`/un nouvel appel réseau à partir de la seule IP.

**TP — squelette attendu** :
```kotlin
@Test
fun `selectRouter bascule la selection`() {
    val component = DefaultDetectionComponent(
        componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
        onConnectClicked = {},
    )
    val router = Routeur(ipAddress = "192.168.1.1", /* ... autres champs ... */)

    component.selectRouter(router)
    assertEquals(router, component.state.value.selectedRouter)

    component.selectRouter(router)
    assertNull(component.state.value.selectedRouter)
}
```
**Grille (sur 10)** :
- Réflexion correcte sur la sérialisation ET la sécurité (5 pts)
- Test fonctionnel, sans dépendance réseau/stockage réelle (3 pts)
- Les deux assertions (sélection puis désélection) présentes (2 pts)

**Barème global du devoir** : 24-30 pts = migration totalement maîtrisée, prêt·e à migrer l'écran `Login` en autonomie · 15-23 pts = bases solides, revoir la section 2.5 sur l'encapsulation · < 15 pts = revoir la section 2.3 (InstanceHolder) avant de continuer.

</details>