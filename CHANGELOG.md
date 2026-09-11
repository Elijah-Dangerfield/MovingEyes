# Changelog

## [1.1.0](https://github.com/Elijah-Dangerfield/MovingEyes/compare/v1.0.0...v1.1.0) (2026-09-11)


### Features

* a failed purchase says so, and says the right thing ([39d1758](https://github.com/Elijah-Dangerfield/MovingEyes/commit/39d17588373beaeb86fc809bd1fdd7468ebca647))
* a StoreKit configuration file, and a guide to testing purchases ([f2341d5](https://github.com/Elijah-Dangerfield/MovingEyes/commit/f2341d5285a8dabd9799dc0b8e28238fae39d89a))
* an unlock button in the drawer, and QA tools on beta builds ([98bc657](https://github.com/Elijah-Dangerfield/MovingEyes/commit/98bc657f68b986aec6caeb4c721e615d90729e47))


### Bug Fixes

* make Clear entitlement actually clear the entitlement ([9d28359](https://github.com/Elijah-Dangerfield/MovingEyes/commit/9d283591ce21343827aa118454cb4bec9ec770ca))
* report an unsellable build, and let QA reach the paywall while owning it ([d4e8cc9](https://github.com/Elijah-Dangerfield/MovingEyes/commit/d4e8cc9b6d3153b60b6485638349f480cc82d643))
* Sentry reports from dev builds, not just CI ([4735114](https://github.com/Elijah-Dangerfield/MovingEyes/commit/473511478b7c19db38a6c74e8c082ce1b030d099))
* shake worked exactly once per process ([97d776c](https://github.com/Elijah-Dangerfield/MovingEyes/commit/97d776c3450fcec204036e577e74a143c9946084))
* stop CIO winning the Ktor engine race on iOS ([6f65f88](https://github.com/Elijah-Dangerfield/MovingEyes/commit/6f65f88716d6f54a28697089e415afce1d939c60))

## [1.0.0](https://github.com/Elijah-Dangerfield/MovingEyes/compare/v0.1.0...v1.0.0) (2026-09-08)


### ⚠ BREAKING CHANGES

* the server, admin console and auth layer are gone.

### Features

* a canvas with nothing on it until you ask, and no more thirty-second demo ([45e1a65](https://github.com/Elijah-Dangerfield/MovingEyes/commit/45e1a65f0b35dc7698e06a9207f37cea4f28bc99))
* a real colour picker, and a sheet the canvas moves with ([e994ec8](https://github.com/Elijah-Dangerfield/MovingEyes/commit/e994ec80a401671b2289346860c50088943feb51))
* align to edges as well as middles, and say which alignment happened ([c4f981e](https://github.com/Elijah-Dangerfield/MovingEyes/commit/c4f981e3f459f90a53189cd59aea345933efe302))
* almond eyes with curved lids, iris fibres and a limbal ring ([846f352](https://github.com/Elijah-Dangerfield/MovingEyes/commit/846f3527235c27590b2f1d89d7a10fe28da8bad1))
* art-directed presets, and scene miniatures in the drawer ([56a038a](https://github.com/Elijah-Dangerfield/MovingEyes/commit/56a038a32e318cee95ed6a9d7720e213e0e6066c))
* assist a resize the way a drag is assisted ([47b832f](https://github.com/Elijah-Dangerfield/MovingEyes/commit/47b832f48929e5a24befee201219c5fdff58255c))
* direct manipulation on the 1:1 canvas ([c10a01b](https://github.com/Elijah-Dangerfield/MovingEyes/commit/c10a01bbc84bc77cc3562fdc7c69515a7dff1c45))
* display mode and the appliance shims ([849c8a4](https://github.com/Elijah-Dangerfield/MovingEyes/commit/849c8a42815a8c39517647cf8e8969c6694005e9))
* dodge the tablet rail, and give eyes a lash line, a corner and an off-centre pupil ([81f62df](https://github.com/Elijah-Dangerfield/MovingEyes/commit/81f62dfa6cf8f454447a346f1cd76f25050d6785))
* draw all twelve eye styles from a single frame clock ([56d2c44](https://github.com/Elijah-Dangerfield/MovingEyes/commit/56d2c4401b4cadae0699ec26a3c7d13f7a4666f7))
* embed the Safelight design system ([2aac935](https://github.com/Elijah-Dangerfield/MovingEyes/commit/2aac935775f1106c63cb67ec84664b3bd9d22c3c))
* four control panels, a scenes drawer, and live scene persistence ([fe83394](https://github.com/Elijah-Dangerfield/MovingEyes/commit/fe8339429f4dd4cd010fc5d1cef59848d4d3ae93))
* measure the cuts, find sounds by arrival time, and stop nesting surfaces ([ffdd579](https://github.com/Elijah-Dangerfield/MovingEyes/commit/ffdd57997820c170bb901d6f03e218f97564cc45))
* one floating rail, three free presets, and nothing left to save ([2ad2132](https://github.com/Elijah-Dangerfield/MovingEyes/commit/2ad2132ef3cf3d8f789ccd7716cfa22ded4c9421))
* one nervous system per scene, a drag-to-delete target, and a reachable sheet ([68304a1](https://github.com/Elijah-Dangerfield/MovingEyes/commit/68304a106359022673f8f071d2ea1f040f73b7e4))
* pair surface with content colour, fix the rename keyboard, measure point to point ([c71771a](https://github.com/Elijah-Dangerfield/MovingEyes/commit/c71771ac4f5d70b64d30d422148010b74eab21f9))
* paywall, settings, and the gating that connects them ([33709b5](https://github.com/Elijah-Dangerfield/MovingEyes/commit/33709b5eccd53e3de637f4c30391296417df5cb9))
* restore Grafana observability and remote config ([e2df36c](https://github.com/Elijah-Dangerfield/MovingEyes/commit/e2df36ce88cebe4294fdcff19ef785d1ce7e7496))
* route user-facing copy through :libraries:resources ([d13ec22](https://github.com/Elijah-Dangerfield/MovingEyes/commit/d13ec2251fb3a087a65693d692d4fa6b6f16950e))
* scene model, persistence, and the thirty-second demo ([938c66a](https://github.com/Elijah-Dangerfield/MovingEyes/commit/938c66a4d271a80682cb11c8060457c9d3928321))
* single non-consumable unlock with a fake store on debug ([d037cd2](https://github.com/Elijah-Dangerfield/MovingEyes/commit/d037cd228290a77f455aba1262faed7557938a4d))
* sound reactivity, with the direction logic and both platform capture paths ([df78584](https://github.com/Elijah-Dangerfield/MovingEyes/commit/df78584822ccc77a1df200bc8d4ac45cb4702cfe))


### Bug Fixes

* a pair is the atom, and the whole screen belongs to a drag ([02687c3](https://github.com/Elijah-Dangerfield/MovingEyes/commit/02687c3501bc0921db362b8c01ad30272a3f1428))
* black splash, styles that replace the eye, and readable dimension lines ([3f5d407](https://github.com/Elijah-Dangerfield/MovingEyes/commit/3f5d407576a47f642b322143b4de04678da0f8a6))
* clamp the panel's occupied height so hiding it can't crash ([e02aefc](https://github.com/Elijah-Dangerfield/MovingEyes/commit/e02aefceecedbd2701a814d97f1f08a863b4c3b8))
* correct the Pages hostname and file the plan under Purchases ([7ae61e4](https://github.com/Elijah-Dangerfield/MovingEyes/commit/7ae61e42125c3de861e935c9a9f60268ec78301a))
* detect sounds by how unusual they are, not how many times the floor ([fb5e092](https://github.com/Elijah-Dangerfield/MovingEyes/commit/fb5e092cb6a42f6a022ba23d55a0c710b894114f))
* feed the analyser fixed-size buffers, whatever the platform delivers ([ab0503a](https://github.com/Elijah-Dangerfield/MovingEyes/commit/ab0503a9c2f9bf8fd13742af05ecaa5080c7a982))
* gestures held a dead editor, and restore the bug report Phase 0 deleted ([ff9588b](https://github.com/Elijah-Dangerfield/MovingEyes/commit/ff9588bb951fb5c98d4c3eaa68cef1a8f7d62125))
* iOS audio capture never delivered a buffer ([8c7e386](https://github.com/Elijah-Dangerfield/MovingEyes/commit/8c7e386695d40e8eafeebad38bdcf80b222b7f05))
* legible status bar, an honest purchase flow, and copy that says something ([8100362](https://github.com/Elijah-Dangerfield/MovingEyes/commit/8100362d38b1ca15399df79487d87f0c900e253f))
* make the editor behave the way it looks like it should ([233f728](https://github.com/Elijah-Dangerfield/MovingEyes/commit/233f728c999a9aada3e86b5a9a5f9dde72db2e20))
* make the iOS app compile for the first time ([139338e](https://github.com/Elijah-Dangerfield/MovingEyes/commit/139338ee8d5f94b2f68ce8c25f74d629d76b9a75))
* match the IAP id the stores actually have, and make the policy true ([fef9ca0](https://github.com/Elijah-Dangerfield/MovingEyes/commit/fef9ca0faff0fefb7870455c29cafb058a95cd87))
* normalise apostrophes and cut an em dash from user-facing copy ([ac2b977](https://github.com/Elijah-Dangerfield/MovingEyes/commit/ac2b977d371a182a783e85de3756209ec7fbc58d))
* put engine.prepare() back beside start(), it crashed on launch ([31d1390](https://github.com/Elijah-Dangerfield/MovingEyes/commit/31d1390d1a82a3788670f3ab138aa6612e6c54b0))
* re-check the store price and the grant on every foreground ([35eae63](https://github.com/Elijah-Dangerfield/MovingEyes/commit/35eae63caad3c9d452262caec61c2cd155356d63))
* register a NavType for PaywallTrigger, which crashed iOS at launch ([10c20c6](https://github.com/Elijah-Dangerfield/MovingEyes/commit/10c20c67cef1d2c0ef5b9e8a6825125d68e767dd))
* sound reactivity was inaudible, unreachable, and adapting against itself ([901ebe8](https://github.com/Elijah-Dangerfield/MovingEyes/commit/901ebe838df8c320567ab754e15b872e02d2030d))
* stop drawing a lid the cardboard already provides, and make dialogs visible ([36d5e4f](https://github.com/Elijah-Dangerfield/MovingEyes/commit/36d5e4f65e70a99c37ececf6f4642d81d8ba45e4))
* stop the readout giving instructions, and unstick the ROTATE label ([7489b53](https://github.com/Elijah-Dangerfield/MovingEyes/commit/7489b53cce98a30281d86f104e0994fea3536b5f))
* the fade covered the content, the pair looked in two directions, and scenes had no name ([720bb74](https://github.com/Elijah-Dangerfield/MovingEyes/commit/720bb742195f8e2e984facce4d7a087816eaa00b))
* the rail panel measured itself on the wrong axis ([8780ca4](https://github.com/Elijah-Dangerfield/MovingEyes/commit/8780ca44b61c67763e01e90f1d6fa17629bc5528))
* two apostrophes rendered with a literal backslash ([9370646](https://github.com/Elijah-Dangerfield/MovingEyes/commit/9370646c5b6cfcbd91cf167dd9fb459f700dcb25))


### Performance Improvements

* measure Compose skippability rather than assume it ([4ab0afc](https://github.com/Elijah-Dangerfield/MovingEyes/commit/4ab0afcd331a65016a4a31b5c59ea2f391012448))
* stop ten components recomposing on every animation frame ([f164e14](https://github.com/Elijah-Dangerfield/MovingEyes/commit/f164e14822e9ae67d11b772f42cb642ad03ecb06))


### Code Refactoring

* strip the template's server, auth and sync stack ([4c3a621](https://github.com/Elijah-Dangerfield/MovingEyes/commit/4c3a62122cfb35a9c3c930d20e7a8fd384502c4e))


### Build System

* let release-please actually write the version it tags ([503d1bf](https://github.com/Elijah-Dangerfield/MovingEyes/commit/503d1bfa209e7d73193d4ec727bf90cac7c45832))

## 0.1.0

Initial version.
