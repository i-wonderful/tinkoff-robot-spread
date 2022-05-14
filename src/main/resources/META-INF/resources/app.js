import * as pages from './pages/index.js'
import homepage from './pages/home.js'
import store from './store.js'

export default {
    name: 'App',
    components: Object.assign({homepage}, pages),

    setup() {
        const {watchEffect, onMounted, ref} = Vue;
        const page = ref(null);

        //store management: save $variables to localstorage
        onMounted(() => {
            window.addEventListener('beforeunload', () => {
                Object.keys(store).forEach(function (key) {
                    if (key.charAt(0) == "$") {
                        localStorage.setItem(key, store[key]);
                    } else {
                        localStorage.removeItem("$" + key);
                    }
                });
            });
            Object.keys(store).forEach(function (key) {
                    if (key.charAt(0) == "$") {
                        if (localStorage.getItem(key)) store[key] = localStorage.getItem(key);
                    }
                }
            )
        })
        //url management
        watchEffect(() => {
            const urlpage = window.location.pathname.split("/").pop();
            if (page.value == null) {
                page.value = urlpage
            }
            if (page.value != urlpage) {
                const url = page.value ? page.value : './';
                window.history.pushState({url: url}, '', url);
            }
            window.onpopstate = function () {
                page.value = window.location.pathname.split("/").pop()
            };
        })

        return {
            page, pages
        };
    },
    template: `
        <div id="sidebar">
            <nav class="nav">
                <div class="nav-left">
                    <div class="tabs">
                    <button class="active" v-on:click="page = ''" class="button button-clear">
                        Home
                    </button>
                    <template v-for="item, index in pages" key="item.name">
                        <button v-on:click="page = index" class="button button-clear">
                            {{ item.name }}
                        </button>
                    </template>
                    </div>
                </div>               
            </nav>
       </div>
        <div id="content">
            <component :is="page || 'homepage'"></component>
        </div>
    `,
};